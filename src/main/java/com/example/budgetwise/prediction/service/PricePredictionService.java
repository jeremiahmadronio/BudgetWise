package com.example.budgetwise.prediction.service;

import com.example.budgetwise.market.entity.MarketLocation;
import com.example.budgetwise.market.repository.MarketLocationRepository;
import com.example.budgetwise.prediction.dto.PriceCalibrationDTO;
import com.example.budgetwise.prediction.entity.PricePredictions;
import com.example.budgetwise.prediction.repository.PricePredictionRepository;
import com.example.budgetwise.product.entity.DailyPriceRecord;
import com.example.budgetwise.product.entity.ProductInfo;
import com.example.budgetwise.product.repository.DailyPriceRecordRepository;
import com.example.budgetwise.product.repository.ProductInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
@Service
@RequiredArgsConstructor
@Slf4j
public class PricePredictionService {

    private final PricePredictionRepository predictionRepo;
    private final DailyPriceRecordRepository priceRepo;
    private final ProductInfoRepository productRepo;
    private final MarketLocationRepository marketRepo;

    @Transactional
    public void generateForecast(Long productId, Long marketId) {
        List<DailyPriceRecord> history = priceRepo
                .findTop30ByProductInfoIdAndMarketLocationIdOrderByPriceReport_DateReportedDesc(
                        productId, marketId);

        if (history.size() < 5) {
            log.warn("Insufficient data for product {} in market {}", productId, marketId);
            return;
        }

        LocalDate lastDate = history.get(0).getPriceReport().getDateReported();
        SimpleRegression regression = new SimpleRegression();

        for (int i = 0; i < history.size(); i++) {
            regression.addData(i, history.get(history.size() - 1 - i).getPrice());
        }

        for (int day = 1; day <= 7; day++) {
            double rawForecast = regression.predict(history.size() + day - 1);
            double finalPrice = Math.max(0.0, rawForecast);
            double confidence = Double.isNaN(regression.getRSquare()) ?
                    0.0 : regression.getRSquare();

            upsertPrediction(
                    history.get(0).getProductInfo(),
                    history.get(0).getMarketLocation(),
                    finalPrice,
                    lastDate.plusDays(day),
                    confidence
            );
        }
    }

    private void upsertPrediction(ProductInfo p, MarketLocation m,
                                  double price, LocalDate date, double conf) {
        PricePredictions pred = predictionRepo
                .findByProductInfoAndMarketLocationAndTargetDate(p, m, date)
                .orElse(new PricePredictions());

        if (pred.getStatus() == PricePredictions.Status.OVERRIDDEN) {
            log.info("Skipping overridden prediction for {} at {}",
                    p.getProductName(), date);
            return;
        }

        pred.setProductInfo(p);
        pred.setMarketLocation(m);
        pred.setPredictedPrice(price);
        pred.setConfidenceScore(conf);
        pred.setTargetDate(date);
        pred.setStatus(PricePredictions.Status.NORMAL);

        predictionRepo.save(pred);
    }

    @Async
    @Transactional
    public void runBulkPrediction() {
        List<Object[]> pairs = priceRepo.findExistingProductMarketPairs();
        log.info("Processing {} active product-market pairs...", pairs.size());

        for (Object[] pair : pairs) {
            try {
                generateForecast((Long) pair[0], (Long) pair[1]);
            } catch (Exception e) {
                log.error("Failed to generate forecast for product {} in market {}",
                        pair[0], pair[1], e);
            }
        }

        log.info("Bulk prediction completed");
    }

    @Transactional(readOnly = true)
    public Page<PriceCalibrationDTO> getCalibrationTable(Long marketId, Pageable pageable) {
        // Validate market exists
        if (!marketRepo.existsById(marketId)) {
            throw new IllegalArgumentException("Market not found: " + marketId);
        }

        LocalDate tomorrow = LocalDate.now().plusDays(1);

        return productRepo.findAllByStatus(ProductInfo.Status.ACTIVE, pageable)
                .map(product -> {
                    Double current = priceRepo
                            .findLatestPriceByProductAndMarket(product.getId(), marketId)
                            .orElse(0.0);

                    PricePredictions pred = predictionRepo
                            .findLatestPrediction(product.getId(), marketId, tomorrow)
                            .orElse(null);

                    Double forecast = (pred != null) ? pred.getPredictedPrice() : 0.0;
                    double trend = (current > 0) ?
                            ((forecast - current) / current) * 100 : 0.0;

                    return new PriceCalibrationDTO(
                            product.getId(),
                            product.getProductName(),
                            current,
                            forecast,
                            trend,
                            pred != null ? pred.getConfidenceScore() : 0.0,
                            pred != null ? pred.getStatus().name() : "PENDING"
                    );
                });
    }
}