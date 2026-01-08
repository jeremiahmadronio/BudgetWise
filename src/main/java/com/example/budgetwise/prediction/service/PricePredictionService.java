package com.example.budgetwise.prediction.service;

import com.example.budgetwise.market.entity.MarketLocation;
import com.example.budgetwise.prediction.dto.*;
import com.example.budgetwise.prediction.entity.PricePredictions;
import com.example.budgetwise.prediction.repository.DailyPriceRecordPredictionRepository;
import com.example.budgetwise.prediction.repository.MarketLocationPredictionRepository;
import com.example.budgetwise.prediction.repository.PricePredictionRepository;
import com.example.budgetwise.prediction.repository.ProductInfoPredictionRepository;
import com.example.budgetwise.product.entity.DailyPriceRecord;
import com.example.budgetwise.product.entity.ProductInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PricePredictionService {

    private final PricePredictionRepository predictionRepo;
    private final DailyPriceRecordPredictionRepository priceRepo;
    private final ProductInfoPredictionRepository productRepo;
    private final MarketLocationPredictionRepository marketRepo;

    private final ExecutorService executorService = Executors.newFixedThreadPool(
            Math.max(4, Runtime.getRuntime().availableProcessors())
    );

    // ============================================================================
// FIXED METHOD 1: generateForecast - Fixed confidence calculation
// ============================================================================
    @Transactional
    public void generateForecast(Long productId, Long marketId) {
        List<DailyPriceRecord> history = priceRepo
                .findTop30ByProductInfoIdAndMarketLocationIdOrderByPriceReport_DateReportedDesc(
                        productId, marketId);

        if (history.isEmpty() || history.size() < 14) {
            log.warn("Insufficient data for product {} in market {} (found {} records)",
                    productId, marketId, history.size());
            return;
        }

        LocalDate lastDate = history.get(0).getPriceReport().getDateReported();
        double currentPrice = history.get(0).getPrice();

        SimpleRegression regression = new SimpleRegression();

        // Build regression model (oldest to newest)
        for (int i = 0; i < history.size(); i++) {
            int historyIndex = history.size() - 1 - i;
            double price = history.get(historyIndex).getPrice();
            regression.addData(i, price);
        }

        double slope = regression.getSlope();
        double rSquare = regression.getRSquare();

        // ========================================================================
        // IMPROVED STATISTICS CALCULATION
        // ========================================================================
        double avgPrice = history.stream()
                .mapToDouble(DailyPriceRecord::getPrice)
                .average()
                .orElse(0.0);

        double variance = history.stream()
                .mapToDouble(record -> Math.pow(record.getPrice() - avgPrice, 2))
                .average()
                .orElse(0.0);

        double stdDev = Math.sqrt(variance);
        double coefficientOfVariation = (avgPrice > 0) ? (stdDev / avgPrice) * 100 : 0;

        // Calculate Mean Absolute Percentage Error (MAPE) for recent predictions
        double mape = calculateMAPE(history, regression);

        // ✅ IMPROVED CONFIDENCE CALCULATION
        double baseConfidence = calculateImprovedConfidence(
                rSquare,
                coefficientOfVariation,
                mape,
                history.size()
        );

        if (log.isDebugEnabled()) {
            log.debug("Product {}, Market {} - Points: {}, R²: {:.3f}, CV: {:.2f}%, MAPE: {:.2f}%, BaseConf: {:.1f}%",
                    productId, marketId, history.size(), rSquare,
                    coefficientOfVariation, mape, baseConfidence * 100);
        }

        // Batch create predictions
        List<PricePredictions> predictions = new ArrayList<>();

        for (int day = 1; day <= 7; day++) {
            double rawForecast = regression.predict(history.size() + day - 1);
            double finalPrice = Math.max(0.0, rawForecast);

            // Decrease confidence for far future predictions
            double dayConfidence = baseConfidence * (1.0 - (day - 1) * 0.03); // 3% decrease per day
            dayConfidence = Math.max(0.30, Math.min(1.0, dayConfidence)); // Floor at 30%

            // Calculate price change percentage
            double priceChange = ((finalPrice - currentPrice) / currentPrice) * 100;

            // ✅ FIXED ANOMALY DETECTION
            PricePredictions.Status status = determineStatus(
                    priceChange,
                    dayConfidence,
                    coefficientOfVariation,
                    day,
                    mape
            );

            PricePredictions pred = createPrediction(
                    history.get(0).getProductInfo(),
                    history.get(0).getMarketLocation(),
                    finalPrice,
                    lastDate.plusDays(day),
                    dayConfidence,
                    status
            );

            if (pred != null) {
                predictions.add(pred);
            }
        }

        if (!predictions.isEmpty()) {
            predictionRepo.saveAll(predictions);
            log.debug("Saved {} predictions for product {} in market {} (confidence: {:.1f}%, anomalies: {})",
                    predictions.size(), productId, marketId, baseConfidence * 100,
                    predictions.stream().filter(p -> p.getStatus() == PricePredictions.Status.ANOMALY).count());
        }
    }

    // ============================================================================
// NEW HELPER METHOD: Calculate MAPE (prediction accuracy)
// ============================================================================
    private double calculateMAPE(List<DailyPriceRecord> history, SimpleRegression regression) {
        if (history.size() < 5) return 0.0;

        double totalError = 0.0;
        int count = Math.min(10, history.size() - 1); // Check last 10 points

        for (int i = 0; i < count; i++) {
            int idx = i;
            double actualPrice = history.get(idx).getPrice();
            double predictedPrice = regression.predict(history.size() - 1 - idx);

            if (actualPrice > 0) {
                double percentError = Math.abs((actualPrice - predictedPrice) / actualPrice) * 100;
                totalError += percentError;
            }
        }

        return count > 0 ? totalError / count : 0.0;
    }

    // ============================================================================
// NEW HELPER METHOD: Improved Confidence Calculation
// ============================================================================
    private double calculateImprovedConfidence(
            double rSquare,
            double coefficientOfVariation,
            double mape,
            int dataPoints) {

        // Component 1: R² score (0-1) - weight 30%
        double rSquareScore = Double.isNaN(rSquare) ? 0.5 : Math.max(0.0, Math.min(1.0, rSquare));

        // Component 2: Price Stability (based on CV) - weight 30%
        // CV < 10% = very stable (1.0), CV > 50% = very volatile (0.0)
        double stabilityScore;
        if (coefficientOfVariation < 10) {
            stabilityScore = 1.0;
        } else if (coefficientOfVariation > 50) {
            stabilityScore = 0.0;
        } else {
            stabilityScore = 1.0 - ((coefficientOfVariation - 10) / 40.0);
        }

        // Component 3: Prediction Accuracy (based on MAPE) - weight 25%
        // MAPE < 5% = excellent (1.0), MAPE > 25% = poor (0.0)
        double accuracyScore;
        if (mape < 5) {
            accuracyScore = 1.0;
        } else if (mape > 25) {
            accuracyScore = 0.0;
        } else {
            accuracyScore = 1.0 - ((mape - 5) / 20.0);
        }

        // Component 4: Data Sufficiency - weight 15%
        // 14 points = 0.7, 30+ points = 1.0
        double dataSufficiencyScore;
        if (dataPoints >= 30) {
            dataSufficiencyScore = 1.0;
        } else if (dataPoints < 14) {
            dataSufficiencyScore = 0.5;
        } else {
            dataSufficiencyScore = 0.7 + ((dataPoints - 14) / 16.0) * 0.3;
        }

        // Weighted combination
        double confidence = (rSquareScore * 0.30) +
                (stabilityScore * 0.30) +
                (accuracyScore * 0.25) +
                (dataSufficiencyScore * 0.15);

        // Ensure confidence is between 0.0 and 1.0
        return Math.max(0.0, Math.min(1.0, confidence));
    }

// ============================================================================
// FIXED METHOD 2: determineStatus - Less aggressive anomaly detection
// ============================================================================
    /**
     * ✅ FIXED ANOMALY DETECTION: More realistic thresholds
     *
     * Rules (RELAXED):
     * 1. Price change > ±40% = ANOMALY (extreme movement)
     * 2. Price change > ±30% AND confidence < 40% = ANOMALY (big uncertain change)
     * 3. Confidence < 20% = ANOMALY (very unreliable)
     * 4. Coefficient of Variation > 60% = ANOMALY (extreme volatility)
     * 5. Far predictions (5+ days) with confidence < 40% = ANOMALY
     */
    private PricePredictions.Status determineStatus(
            double priceChange,
            double confidence,
            double coefficientOfVariation,
            int daysAhead,
            double mape) {

        // Rule 1: Extreme price movements (>40% change) - RELAXED from 25%
        if (Math.abs(priceChange) > 40.0) {
            log.info("ANOMALY: Extreme price change {:.1f}% (threshold: 40%)", priceChange);
            return PricePredictions.Status.ANOMALY;
        }

        // Rule 2: Large uncertain price movements - RELAXED thresholds
        if (Math.abs(priceChange) > 30.0 && confidence < 0.40) {
            log.info("ANOMALY: Large price change {:.1f}% with low confidence {:.0f}%",
                    priceChange, confidence * 100);
            return PricePredictions.Status.ANOMALY;
        }

        // Rule 3: Very low confidence - RELAXED from 30% to 20%
        if (confidence < 0.20) {
            log.info("ANOMALY: Very low confidence {:.0f}% (threshold: 20%)", confidence * 100);
            return PricePredictions.Status.ANOMALY;
        }

        // Rule 4: Extreme price volatility - RELAXED from 40% to 60%
        if (coefficientOfVariation > 60.0) {
            log.info("ANOMALY: High volatility CV={:.1f}% (threshold: 60%)", coefficientOfVariation);
            return PricePredictions.Status.ANOMALY;
        }

        // Rule 5: Far future predictions with low confidence - RELAXED threshold
        if (daysAhead >= 5 && confidence < 0.40) {
            log.debug("ANOMALY: Far prediction (day {}) with low confidence {:.0f}%",
                    daysAhead, confidence * 100);
            return PricePredictions.Status.ANOMALY;
        }

        // Rule 6: High prediction error (MAPE > 30%)
        if (mape > 30.0) {
            log.info("ANOMALY: High prediction error MAPE={:.1f}% (threshold: 30%)", mape);
            return PricePredictions.Status.ANOMALY;
        }

        return PricePredictions.Status.NORMAL;
    }

    // ============================================================================
// KEEP EXISTING: createPrediction with status parameter (no changes needed)
// ============================================================================
    private PricePredictions createPrediction(
            ProductInfo p,
            MarketLocation m,
            double price,
            LocalDate date,
            double conf,
            PricePredictions.Status status) {

        PricePredictions pred = predictionRepo
                .findByProductInfoAndMarketLocationAndTargetDate(p, m, date)
                .orElse(new PricePredictions());

        // Don't overwrite manual overrides
        if (pred.getId() != null && pred.getStatus() == PricePredictions.Status.OVERRIDDEN) {
            log.debug("Skipping overridden prediction for {} at {}", p.getProductName(), date);
            return null;
        }

        pred.setProductInfo(p);
        pred.setMarketLocation(m);
        pred.setPredictedPrice(price);
        pred.setConfidenceScore(conf);
        pred.setTargetDate(date);
        pred.setStatus(status);

        return pred;
    }

   

    @Async
    @Transactional
    public void runBulkPrediction() {
        List<Object[]> pairs = priceRepo.findExistingProductMarketPairs();
        log.info("Processing {} active product-market pairs...", pairs.size());

        int batchSize = 50; // Reduced batch size for better transaction management
        List<List<Object[]>> batches = partitionList(pairs, batchSize);

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (List<Object[]> batch : batches) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                processBatch(batch);
            }, executorService);

            futures.add(future);
        }

        // Wait for all batches to complete
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();
            log.info("Bulk prediction completed successfully - processed {} pairs", pairs.size());
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error during bulk prediction", e);
            Thread.currentThread().interrupt();
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processBatch(List<Object[]> batch) {
        int success = 0;
        int failed = 0;

        for (Object[] pair : batch) {
            try {
                generateForecast((Long) pair[0], (Long) pair[1]);
                success++;
            } catch (Exception e) {
                failed++;
                log.error("Failed to generate forecast for product {} in market {}: {}",
                        pair[0], pair[1], e.getMessage());
            }
        }

        log.info("Batch completed - Success: {}, Failed: {}", success, failed);
    }

    private <T> List<List<T>> partitionList(List<T> list, int size) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
    }

    @Transactional(readOnly = true)
    public Page<PriceCalibrationDTO> getCalibrationTable(Long marketId, Pageable pageable) {
        MarketLocation market = marketRepo.findById(marketId)
                .orElseThrow(() -> new IllegalArgumentException("Market not found: " + marketId));

        LocalDate tomorrow = LocalDate.now().plusDays(1);

        return productRepo.findAllByStatus(ProductInfo.Status.ACTIVE, pageable)
                .map(product -> {
                    Double current = priceRepo
                            .findLatestPriceByProductAndMarket(product.getId(), marketId)
                            .orElse(null);

                    PricePredictions pred = predictionRepo
                            .findLatestPrediction(product.getId(), marketId, tomorrow)
                            .orElse(null);

                    Double forecast = (pred != null) ? pred.getPredictedPrice() : null;

                    Double trend = null;
                    if (current != null && forecast != null && current > 0) {
                        trend = ((forecast - current) / current) * 100;
                    }

                    return new PriceCalibrationDTO(
                            product.getId(),
                            product.getProductName(),
                            market.getId(),
                            market.getMarketLocation(),
                            current,
                            forecast,
                            trend,
                            pred != null ? pred.getConfidenceScore() : null,
                            pred != null ? pred.getStatus().name() : "NO_DATA"
                    );
                });
    }
    /**
     * DEBUG METHOD: Get historical price data and regression analysis
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getDebugHistoricalData(Long productId, Long marketId) {
        List<DailyPriceRecord> history = priceRepo
                .findTop30ByProductInfoIdAndMarketLocationIdOrderByPriceReport_DateReportedDesc(
                        productId, marketId);

        Map<String, Object> result = new HashMap<>();
        result.put("productId", productId);
        result.put("marketId", marketId);
        result.put("dataPoints", history.size());

        if (history.isEmpty()) {
            result.put("error", "No historical data found");
            return result;
        }

        // Historical prices (as stored - newest first)
        List<Map<String, Object>> rawHistory = new ArrayList<>();
        for (DailyPriceRecord record : history) {
            Map<String, Object> point = new HashMap<>();
            point.put("date", record.getPriceReport().getDateReported());
            point.put("price", record.getPrice());
            rawHistory.add(point);
        }
        result.put("rawHistory", rawHistory);

        // Regression input (oldest to newest)
        List<Map<String, Object>> regressionInput = new ArrayList<>();
        SimpleRegression regression = new SimpleRegression();

        for (int i = 0; i < history.size(); i++) {
            int historyIndex = history.size() - 1 - i;
            DailyPriceRecord record = history.get(historyIndex);
            double price = record.getPrice();

            regression.addData(i, price);

            Map<String, Object> point = new HashMap<>();
            point.put("x", i);
            point.put("date", record.getPriceReport().getDateReported());
            point.put("price", price);
            regressionInput.add(point);
        }
        result.put("regressionInput", regressionInput);

        // Regression stats
        Map<String, Object> stats = new HashMap<>();
        stats.put("slope", regression.getSlope());
        stats.put("intercept", regression.getIntercept());
        stats.put("rSquare", regression.getRSquare());
        stats.put("slopeDirection", regression.getSlope() > 0 ? "UPWARD" : "DOWNWARD");
        result.put("regressionStats", stats);

        // Predictions
        List<Map<String, Object>> predictions = new ArrayList<>();
        LocalDate lastDate = history.get(0).getPriceReport().getDateReported();

        for (int day = 1; day <= 7; day++) {
            double forecast = regression.predict(history.size() + day - 1);
            Map<String, Object> pred = new HashMap<>();
            pred.put("day", day);
            pred.put("date", lastDate.plusDays(day));
            pred.put("predictedPrice", Math.max(0.0, forecast));
            predictions.add(pred);
        }
        result.put("predictions", predictions);

        // Current vs Predicted
        double currentPrice = history.get(0).getPrice();
        double tomorrowPrice = regression.predict(history.size());
        result.put("currentPrice", currentPrice);
        result.put("tomorrowPrice", Math.max(0.0, tomorrowPrice));
        result.put("change", tomorrowPrice - currentPrice);
        result.put("changePercent", ((tomorrowPrice - currentPrice) / currentPrice) * 100);

        return result;
    }



    /**
     * ENTERPRISE METHOD: Get all active markets with statistics
     * For dropdown/selector in UI
     */
    @Transactional(readOnly = true)
    public List<MarketInfoDTO> getActiveMarkets() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        return marketRepo.findAll().stream()
                .filter(market -> market.getStatus() == MarketLocation.Status.ACTIVE)
                .map(market -> {
                    // Count products that have price data in this market
                    Integer productCount = priceRepo
                            .countDistinctProductsByMarketId(market.getId());

                    // Count predictions for tomorrow in this market
                    Integer predictionCount = predictionRepo
                            .countByMarketLocationIdAndTargetDate(market.getId(), tomorrow);

                    // Count anomalies in this market
                    Integer anomalyCount = predictionRepo
                            .countByMarketLocationIdAndTargetDateAndStatus(
                                    market.getId(),
                                    tomorrow,
                                    PricePredictions.Status.ANOMALY
                            );

                    return new MarketInfoDTO(
                            market.getId(),
                            market.getMarketLocation(), // This is the name field
                            String.format("%s Market (%.4f, %.4f)",
                                    market.getType().name(),
                                    market.getLatitude(),
                                    market.getLongitude()),
                            productCount,
                            predictionCount,
                            anomalyCount
                    );
                })
                .filter(m -> m.productCount() > 0)  // Only markets with data
                .collect(Collectors.toList());
    }


    @Transactional(readOnly = true)
    public Page<ProductCentricPredictionDTO> getProductCentricPredictions(Pageable pageable) {
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        return productRepo.findAllByStatus(ProductInfo.Status.ACTIVE, pageable)
                .map(product -> buildProductCentricDTO(product, tomorrow));
    }

    @Transactional(readOnly = true)
    public List<ProductCentricPredictionDTO> getComparisonMatrix(List<Long> productIds) {
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        // If no IDs provided, get top 20 products
        if (productIds == null || productIds.isEmpty()) {
            productIds = productRepo.findAllByStatus(
                            ProductInfo.Status.ACTIVE,
                            PageRequest.of(0, 20)
                    )
                    .stream()
                    .map(ProductInfo::getId)
                    .collect(Collectors.toList());
        }

        return productIds.stream()
                .map(productId -> {
                    ProductInfo product = productRepo.findById(productId)
                            .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
                    return buildProductCentricDTO(product, tomorrow);
                })
                .collect(Collectors.toList());
    }

    /**
     * Helper method: Build product-centric DTO with all market data
     */
    private ProductCentricPredictionDTO buildProductCentricDTO(ProductInfo product, LocalDate targetDate) {
        List<MarketLocation> allMarkets = marketRepo.findAll().stream()
                .filter(m -> m.getStatus() == MarketLocation.Status.ACTIVE)
                .collect(Collectors.toList());

        // Get predictions for all markets
        List<ProductCentricPredictionDTO.MarketPrediction> marketPredictions =
                allMarkets.stream()
                        .map(market -> createMarketPrediction(product, market, targetDate))
                        .filter(mp -> mp.currentPrice() != null)  // Only markets with data
                        .collect(Collectors.toList());

        // Calculate aggregate statistics
        double avgCurrent = marketPredictions.stream()
                .filter(mp -> mp.currentPrice() != null)
                .mapToDouble(ProductCentricPredictionDTO.MarketPrediction::currentPrice)
                .average()
                .orElse(0.0);

        double avgForecast = marketPredictions.stream()
                .filter(mp -> mp.forecastPrice() != null)
                .mapToDouble(ProductCentricPredictionDTO.MarketPrediction::forecastPrice)
                .average()
                .orElse(0.0);

        Double maxPrice = marketPredictions.stream()
                .filter(mp -> mp.currentPrice() != null)
                .map(ProductCentricPredictionDTO.MarketPrediction::currentPrice)
                .max(Double::compareTo)
                .orElse(null);

        Double minPrice = marketPredictions.stream()
                .filter(mp -> mp.currentPrice() != null)
                .map(ProductCentricPredictionDTO.MarketPrediction::currentPrice)
                .min(Double::compareTo)
                .orElse(null);

        Double priceDiff = (maxPrice != null && minPrice != null) ? maxPrice - minPrice : null;

        String mostExpensive = marketPredictions.stream()
                .filter(mp -> mp.currentPrice() != null)
                .max(Comparator.comparing(ProductCentricPredictionDTO.MarketPrediction::currentPrice))
                .map(ProductCentricPredictionDTO.MarketPrediction::marketName)
                .orElse(null);

        String cheapest = marketPredictions.stream()
                .filter(mp -> mp.currentPrice() != null)
                .min(Comparator.comparing(ProductCentricPredictionDTO.MarketPrediction::currentPrice))
                .map(ProductCentricPredictionDTO.MarketPrediction::marketName)
                .orElse(null);

        long anomalyCount = marketPredictions.stream()
                .filter(mp -> "ANOMALY".equals(mp.status()))
                .count();

        return new ProductCentricPredictionDTO(
                
                product.getId(),
                product.getProductName(),
                String.format("PROD-%03d", product.getId()), // Generate product code
                product.getCategory() != null ? product.getCategory() : "UNKNOWN",
                marketPredictions,
                avgCurrent,
                avgForecast,
                priceDiff,
                mostExpensive,
                cheapest,
                marketPredictions.size(),
                (int) anomalyCount
        );
    }

    /**
     * Helper method: Create market prediction for a specific product-market pair
     */
    private ProductCentricPredictionDTO.MarketPrediction createMarketPrediction(
            ProductInfo product, MarketLocation market, LocalDate targetDate) {

        Double current = priceRepo
                .findLatestPriceByProductAndMarket(product.getId(), market.getId())
                .orElse(null);

        PricePredictions pred = predictionRepo
                .findLatestPrediction(product.getId(), market.getId(), targetDate)
                .orElse(null);

        Double forecast = (pred != null) ? pred.getPredictedPrice() : null;

        Double trend = null;
        if (current != null && forecast != null && current > 0) {
            trend = ((forecast - current) / current) * 100;
        }

        Integer dataPoints = priceRepo
                .countByProductInfoIdAndMarketLocationId(product.getId(), market.getId());

        return new ProductCentricPredictionDTO.MarketPrediction(
                pred != null ? pred.getId() : null, 
                market.getId(),
                market.getMarketLocation(), // This is the name
                String.format("%s (%.4f, %.4f)",
                        market.getType().name(),
                        market.getLatitude(),
                        market.getLongitude()),
                current,
                forecast,
                trend,
                pred != null ? pred.getConfidenceScore() : null,
                pred != null ? pred.getStatus().name() : "NO_DATA",
                dataPoints
        );
    }



    @Transactional(readOnly = true)
    public DashboardStatsDTO getDashboardStats() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        //  These stay the same
        Integer totalProducts = productRepo.countActiveProducts();
        Integer activeMarkets = marketRepo.countActiveMarkets();

        Integer totalPredictions = predictionRepo.countUniquePredictionsByDate(tomorrow);
        Integer anomalies = predictionRepo.countLatestAnomaliesByDate(tomorrow);
        Double avgConfidence = predictionRepo.calculateAverageConfidenceFromLatest(tomorrow);

        Double modelAccuracy = (avgConfidence != null) ? avgConfidence * 100 : null;

        //  This stays the same
        LocalDateTime lastUpdate = predictionRepo.findLatestPredictionTime();
        String lastUpdated = null;
        if (lastUpdate != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
            lastUpdated = lastUpdate.format(formatter);
        }

        return new DashboardStatsDTO(
                totalProducts,
                activeMarkets,
                modelAccuracy,
                anomalies,
                totalPredictions,
                lastUpdated
        );
    }


    @Transactional
    public BulkManualOverrideResponseDTO applyBulkManualOverride(BulkManualOverrideRequestDTO request) {
        if (!request.isValid()) {
            return new BulkManualOverrideResponseDTO(false, "Invalid request", 0, 0, 0, List.of());
        }

        LocalDate targetDate = request.targetDate() != null ? request.targetDate() : LocalDate.now().plusDays(1);
        List<BulkManualOverrideRequestDTO.ProductMarketPair> pairsToProcess = collectPairsToProcess(request);

        if (pairsToProcess.isEmpty()) {
            return new BulkManualOverrideResponseDTO(false, "No valid pairs found", 0, 0, 0, List.of());
        }

        // --- STEP 1: PRE-FETCH DATA (Batch Loading) ---
        Set<Long> productIds = pairsToProcess.stream().map(p -> p.productId()).collect(Collectors.toSet());
        Set<Long> marketIds = pairsToProcess.stream().map(p -> p.marketId()).collect(Collectors.toSet());

        // Map para mabilis ang lookup (O(1)) imbes na mag-DB query sa loob ng loop
        Map<Long, ProductInfo> productMap = productRepo.findAllById(productIds).stream()
                .collect(Collectors.toMap(ProductInfo::getId, p -> p));
        Map<Long, MarketLocation> marketMap = marketRepo.findAllById(marketIds).stream()
                .collect(Collectors.toMap(MarketLocation::getId, m -> m));

        // --- STEP 2: PROCESS ---
        List<BulkManualOverrideResponseDTO.OverrideResult> results = new ArrayList<>();
        int successCount = 0;

        for (var pair : pairsToProcess) {
            try {
                ProductInfo product = productMap.get(pair.productId());
                MarketLocation market = marketMap.get(pair.marketId());

                if (product == null || market == null) {
                    throw new IllegalArgumentException("Invalid product or market ID");
                }

                // Dito mo i-apply ang update logic
                var result = processSingleOverrideOptimized(product, market, targetDate, request);
                results.add(result);
                if (result.success()) successCount++;

            } catch (Exception e) {
                results.add(new BulkManualOverrideResponseDTO.OverrideResult(
                        pair.productId(), null, pair.marketId(), null,
                        false, e.getMessage(), null, null, "FAILED"
                ));
            }
        }

        return new BulkManualOverrideResponseDTO(
                successCount > 0,
                "Processed " + pairsToProcess.size() + " records",
                pairsToProcess.size(), successCount, pairsToProcess.size() - successCount, results
        );
    }

    private List<BulkManualOverrideRequestDTO.ProductMarketPair> collectPairsToProcess(BulkManualOverrideRequestDTO request) {
        // Priority: Specific checkboxes from UI
        if (request.pairs() != null && !request.pairs().isEmpty()) {
            return request.pairs();
        }

        List<BulkManualOverrideRequestDTO.ProductMarketPair> pairs = new ArrayList<>();

        // Logic for other bulk options (simplified)
        List<Long> pIds = (request.productIds() != null) ? request.productIds() :
                (request.productId() != null ? List.of(request.productId()) : List.of());

        // Kung 'overrideAllMarkets' is true, kunin lahat ng active markets
        List<Long> mIds;
        if (Boolean.TRUE.equals(request.overrideAllMarkets())) {
            mIds = marketRepo.findAll().stream()
                    .filter(m -> m.getStatus() == MarketLocation.Status.ACTIVE)
                    .map(MarketLocation::getId).toList();
        } else {
            mIds = (request.marketIds() != null) ? request.marketIds() :
                    (request.marketId() != null ? List.of(request.marketId()) : List.of());
        }

        for (Long pid : pIds) {
            for (Long mid : mIds) {
                pairs.add(new BulkManualOverrideRequestDTO.ProductMarketPair(pid, mid));
            }
        }
        return pairs;
    }
   

  
    private BulkManualOverrideResponseDTO.OverrideResult processSingleOverrideOptimized(
            ProductInfo product,
            MarketLocation market,
            LocalDate targetDate,
            BulkManualOverrideRequestDTO request) {

        try {
            Long productId = product.getId();
            Long marketId = market.getId();

           
            PricePredictions prediction = predictionRepo
                    .findLatestPrediction(productId, marketId, targetDate)
                    .orElseGet(() -> {
                        PricePredictions newPred = new PricePredictions();
                        newPred.setProductInfo(product);
                        newPred.setMarketLocation(market);
                        newPred.setTargetDate(targetDate);
                        return newPred;
                    });

            Double oldPrice = prediction.getPredictedPrice();

            Double currentPrice = priceRepo
                    .findLatestPriceByProductAndMarket(productId, marketId)
                    .orElse(oldPrice != null ? oldPrice : 0.0);

            Double newPrice;
            if (request.manualPrice() != null) {
                newPrice = request.manualPrice();
            } else {
                newPrice = calculateForcedPrice(currentPrice, request.forceTrend());
            }

            // 4. Update Prediction State
            if (prediction.getOverridePrice() == null) {
                prediction.setOverridePrice(oldPrice); 
            }

            prediction.setPredictedPrice(newPrice);
            prediction.setOverrideReason(request.reason());
            prediction.setStatus(PricePredictions.Status.OVERRIDDEN);
            prediction.setConfidenceScore(1.0); // Manual override is always 100% "confident"

            predictionRepo.save(prediction);

            return new BulkManualOverrideResponseDTO.OverrideResult(
                    productId,
                    product.getProductName(),
                    marketId,
                    market.getMarketLocation(),
                    true,
                    "Override applied successfully",
                    oldPrice,
                    newPrice,
                    "OVERRIDDEN"
            );

        } catch (Exception e) {
            log.error("Failed to process optimized override for P:{} M:{}", product.getId(), market.getId(), e);
            return new BulkManualOverrideResponseDTO.OverrideResult(
                    product.getId(), product.getProductName(), market.getId(), market.getMarketLocation(),
                    false, "System Error: " + e.getMessage(), null, null, "FAILED"
            );
        }
    }


    private Double calculateForcedPrice(Double currentPrice, String forceTrend) {
        if (currentPrice == null || currentPrice <= 0) return 0.0;
        if (forceTrend == null || forceTrend.isBlank()) return currentPrice;

        return switch (forceTrend.toUpperCase()) {
            // Standard UI Labels
            case "NO_OVERRIDE", "STABILIZE" -> currentPrice;
            case "+10% INCREASE" -> currentPrice * 1.10;
            case "+20% INCREASE" -> currentPrice * 1.20;
            case "+30% INCREASE" -> currentPrice * 1.30;
            case "+50% INCREASE" -> currentPrice * 1.50;
            case "-10% DECREASE" -> currentPrice * 0.90;
            case "-20% DECREASE" -> currentPrice * 0.80;
            case "-30% DECREASE" -> currentPrice * 0.70;
            case "-50% DECREASE" -> currentPrice * 0.50;

            default -> {
                try {
                    String cleaned = forceTrend.replaceAll("[^0-9.-]", "");

                    if (cleaned.isEmpty() || cleaned.equals("-") || cleaned.equals(".")) {
                        log.warn("Invalid trend format received: {}", forceTrend);
                        yield currentPrice;
                    }

                    double percent = Double.parseDouble(cleaned);

                    double calculated = currentPrice * (1 + (percent / 100));
                    yield Math.max(0.0, calculated);

                } catch (NumberFormatException e) {
                    log.error("Failed to parse dynamic trend: {}", forceTrend);
                    yield currentPrice;
                }
            }
        };
    }






    @Transactional
    public int ManualGenerateForecast(Long productId, Long marketId, boolean forceUpdate) {
        List<DailyPriceRecord> history = priceRepo
                .findTop30ByProductInfoIdAndMarketLocationIdOrderByPriceReport_DateReportedDesc(
                        productId, marketId);

        if (history.isEmpty() || history.size() < 14) {
            log.warn("Insufficient data for product {} in market {} (found {} records)",
                    productId, marketId, history.size());
            return 0;
        }

        LocalDate lastDate = history.get(0).getPriceReport().getDateReported();
        double currentPrice = history.get(0).getPrice();

        SimpleRegression regression = new SimpleRegression();

        for (int i = 0; i < history.size(); i++) {
            int historyIndex = history.size() - 1 - i;
            double price = history.get(historyIndex).getPrice();
            regression.addData(i, price);
        }

        double slope = regression.getSlope();
        double rSquare = regression.getRSquare();

        double avgPrice = history.stream()
                .mapToDouble(DailyPriceRecord::getPrice)
                .average()
                .orElse(0.0);

        double variance = history.stream()
                .mapToDouble(record -> Math.pow(record.getPrice() - avgPrice, 2))
                .average()
                .orElse(0.0);

        double stdDev = Math.sqrt(variance);
        double coefficientOfVariation = (avgPrice > 0) ? (stdDev / avgPrice) * 100 : 0;

        double mape = calculateMAPE(history, regression);

        double baseConfidence = calculateImprovedConfidence(
                rSquare,
                coefficientOfVariation,
                mape,
                history.size()
        );

        if (log.isDebugEnabled()) {
            log.debug("Product {}, Market {} - Points: {}, R²: {:.3f}, CV: {:.2f}%, MAPE: {:.2f}%, BaseConf: {:.1f}%",
                    productId, marketId, history.size(), rSquare,
                    coefficientOfVariation, mape, baseConfidence * 100);
        }

        List<PricePredictions> predictions = new ArrayList<>();

        for (int day = 1; day <= 7; day++) {
            double rawForecast = regression.predict(history.size() + day - 1);
            double finalPrice = Math.max(0.0, rawForecast);

            double dayConfidence = baseConfidence * (1.0 - (day - 1) * 0.03);
            dayConfidence = Math.max(0.30, Math.min(1.0, dayConfidence));

            double priceChange = ((finalPrice - currentPrice) / currentPrice) * 100;

            PricePredictions.Status status = determineStatus(
                    priceChange,
                    dayConfidence,
                    coefficientOfVariation,
                    day,
                    mape
            );

            PricePredictions pred = createPrediction(
                    history.get(0).getProductInfo(),
                    history.get(0).getMarketLocation(),
                    finalPrice,
                    lastDate.plusDays(day),
                    dayConfidence,
                    status,
                    forceUpdate 
            );

            if (pred != null) {
                predictions.add(pred);
            }
        }

        if (!predictions.isEmpty()) {
            predictionRepo.saveAll(predictions);
            predictionRepo.flush(); 

            long anomalyCount = predictions.stream()
                    .filter(p -> p.getStatus() == PricePredictions.Status.ANOMALY)
                    .count();

            log.info(" Saved {} predictions for product {} in market {} (confidence: {:.1f}%, anomalies: {})",
                    predictions.size(), productId, marketId, baseConfidence * 100, anomalyCount);

            return predictions.size();
        }

        log.warn(" No predictions saved for product {} in market {} (all may be overridden or skipped)",
                productId, marketId);
        return 0;
    }


    private PricePredictions createPrediction(
            ProductInfo p,
            MarketLocation m,
            double price,
            LocalDate date,
            double conf,
            PricePredictions.Status status,
            boolean forceUpdate) {

        PricePredictions pred = predictionRepo
                .findByProductInfoAndMarketLocationAndTargetDate(p, m, date)
                .orElse(new PricePredictions());

        if (pred.getId() != null && pred.getStatus() == PricePredictions.Status.OVERRIDDEN) {
            log.info(" REGENERATING: Overwriting manual override for {} on {} as requested by Admin.",
                    p.getProductName(), date);
        }

        pred.setProductInfo(p);
        pred.setMarketLocation(m);
        pred.setPredictedPrice(price); 
        pred.setConfidenceScore(conf);
        pred.setTargetDate(date);

        pred.setStatus(status);

        return pred;
    }

   
}