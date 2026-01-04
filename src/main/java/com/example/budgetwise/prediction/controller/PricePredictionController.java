package com.example.budgetwise.prediction.controller;

import com.example.budgetwise.prediction.dto.*;
import com.example.budgetwise.prediction.service.PricePredictionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/predictions")
@RequiredArgsConstructor
@Validated
@Slf4j
public class PricePredictionController {

    private final PricePredictionService predictionService;

    /**
     * Trigger bulk prediction for all product-market pairs
     * Returns immediately, processing happens asynchronously
     */
    @PostMapping("/bulk-trigger")
    public ResponseEntity<Map<String, Object>> triggerBulk() {
        log.info("Bulk prediction triggered via API");

        CompletableFuture.runAsync(() -> {
            try {
                predictionService.runBulkPrediction();
            } catch (Exception e) {
                log.error("Bulk prediction failed", e);
            }
        });

        Map<String, Object> response = new HashMap<>();
        response.put("status", "STARTED");
        response.put("message", "Bulk market-aware prediction triggered successfully");
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.accepted().body(response);
    }

    /**
     * Generate forecast for specific product-market pair
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateForecast(
            @RequestParam @Positive Long productId,
            @RequestParam @Positive Long marketId) {

        try {
            predictionService.generateForecast(productId, marketId);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("message", "Forecast generated successfully");
            response.put("productId", productId);
            response.put("marketId", marketId);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to generate forecast for product {} in market {}",
                    productId, marketId, e);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "ERROR");
            response.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get calibration table with pagination and sorting
     */
    @GetMapping("/calibration-table/{marketId}")
    public ResponseEntity<Page<PriceCalibrationDTO>> getTable(
            @PathVariable @Positive Long marketId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "productName") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDirection) {

        try {
            Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);

            Page<PriceCalibrationDTO> result = predictionService.getCalibrationTable(marketId, pageable);

            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid market ID: {}", marketId);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error fetching calibration table for market {}", marketId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Batch generate forecasts for multiple product-market pairs
     */
    @PostMapping("/batch-generate")
    public ResponseEntity<Map<String, Object>> batchGenerate(
            @RequestBody @Valid List<ProductMarketPairRequest> pairs) {

        log.info("Batch generate triggered for {} pairs", pairs.size());

        CompletableFuture.runAsync(() -> {
            int success = 0;
            int failed = 0;

            for (ProductMarketPairRequest pair : pairs) {
                try {
                    predictionService.generateForecast(pair.productId(), pair.marketId());
                    success++;
                } catch (Exception e) {
                    log.error("Failed to generate forecast for pair: {}", pair, e);
                    failed++;
                }
            }

            log.info("Batch generation completed - Success: {}, Failed: {}", success, failed);
        });

        Map<String, Object> response = new HashMap<>();
        response.put("status", "STARTED");
        response.put("message", "Batch generation in progress");
        response.put("totalPairs", pairs.size());
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.accepted().body(response);
    }

    /**
     * Get prediction status/health check
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("service", "PricePredictionService");
        status.put("status", "RUNNING");
        status.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(status);
    }


    @GetMapping("/debug/history")
    public ResponseEntity<Map<String, Object>> getHistoricalData(
            @RequestParam @Positive Long productId,
            @RequestParam @Positive Long marketId) {

        log.info("Debug: Fetching historical data for product {} in market {}",
                productId, marketId);

        Map<String, Object> debug = predictionService
                .getDebugHistoricalData(productId, marketId);

        return ResponseEntity.ok(debug);
    }

    /**
     * DEBUG ENDPOINT: Analyze a specific product's prediction
     * Shows why the prediction is going up or down
     * <p>
     * Example: GET /api/v1/predictions/debug/analyze?productId=207&marketId=1
     */
    @GetMapping("/debug/analyze")
    public ResponseEntity<Map<String, Object>> analyzePrediction(
            @RequestParam @Positive Long productId,
            @RequestParam @Positive Long marketId) {

        Map<String, Object> analysis = new HashMap<>();

        try {
            Map<String, Object> debug = predictionService
                    .getDebugHistoricalData(productId, marketId);

            @SuppressWarnings("unchecked")
            Map<String, Object> stats = (Map<String, Object>) debug.get("regressionStats");
            Double slope = (Double) stats.get("slope");
            Double rSquare = (Double) stats.get("rSquare");

            analysis.put("productId", productId);
            analysis.put("marketId", marketId);
            analysis.put("trendDirection", slope > 0 ? "UPWARD ️" : "DOWNWARD");
            analysis.put("slope", slope);
            analysis.put("confidence", rSquare);
            analysis.put("confidenceLevel",
                    rSquare > 0.7 ? "HIGH" :
                            rSquare > 0.4 ? "MEDIUM" : "LOW");

            // Interpretation
            String interpretation;
            if (slope > 0) {
                interpretation = String.format(
                        "Prices are increasing by ₱%.2f per day on average. " +
                                "Expect prices to continue rising.",
                        slope);
            } else {
                interpretation = String.format(
                        "Prices are decreasing by ₱%.2f per day on average. " +
                                "Expect prices to continue falling.",
                        Math.abs(slope));
            }
            analysis.put("interpretation", interpretation);

            // Data quality
            String quality;
            if (rSquare > 0.7) {
                quality = "Excellent - Predictions are reliable";
            } else if (rSquare > 0.4) {
                quality = "Good - Predictions have moderate reliability";
            } else if (rSquare > 0.2) {
                quality = "Fair - Predictions have low reliability";
            } else {
                quality = "Poor - High volatility, predictions unreliable";
            }
            analysis.put("dataQuality", quality);

            analysis.put("fullDebugData", debug);

            return ResponseEntity.ok(analysis);

        } catch (Exception e) {
            log.error("Error analyzing prediction", e);
            analysis.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(analysis);
        }
    }


    /**
     * GET ALL ACTIVE MARKETS
     * Used for market dropdown/selector in UI
     * <p>
     * GET /api/v1/predictions/markets
     */
    @GetMapping("/markets")
    public ResponseEntity<List<MarketInfoDTO>> getActiveMarkets() {
        try {
            List<MarketInfoDTO> markets = predictionService.getActiveMarkets();
            return ResponseEntity.ok(markets);
        } catch (Exception e) {
            log.error("Failed to fetch active markets", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET PRODUCT-CENTRIC VIEW (ENTERPRISE APPROACH - MAIN ENDPOINT)
     * Shows all products with predictions across ALL markets
     * <p>
     * GET /api/v1/predictions/products?page=0&size=10
     */
    @GetMapping("/products")
    public ResponseEntity<Page<ProductCentricPredictionDTO>> getProductCentricView(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "productName") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDirection) {

        try {
            Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);

            Page<ProductCentricPredictionDTO> result = predictionService
                    .getProductCentricPredictions(pageable);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error fetching product-centric predictions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET COMPARISON MATRIX
     * Get specific products with full market comparison
     * <p>
     * GET /api/v1/predictions/comparison-matrix
     * GET /api/v1/predictions/comparison-matrix?productIds=1,2,3
     */
    @GetMapping("/comparison-matrix")
    public ResponseEntity<List<ProductCentricPredictionDTO>> getComparisonMatrix(
            @RequestParam(required = false) List<Long> productIds) {

        try {
            List<ProductCentricPredictionDTO> result = predictionService
                    .getComparisonMatrix(productIds);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error fetching comparison matrix", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET MARKET-CENTRIC VIEW (ALTERNATIVE ENDPOINT)
     * Shows all products for ONE specific market
     * <p>
     * GET /api/v1/predictions/markets/1/predictions?page=0&size=20
     */
    @GetMapping("/markets/{marketId}/predictions")
    public ResponseEntity<Page<PriceCalibrationDTO>> getMarketCentricView(
            @PathVariable @Positive Long marketId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "productName") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDirection) {

        try {
            Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);

            Page<PriceCalibrationDTO> result = predictionService
                    .getCalibrationTable(marketId, pageable);

            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid market ID: {}", marketId);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error fetching market-centric view for market {}", marketId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/dashboard/stats")
    public ResponseEntity<DashboardStatsDTO> getDashboardStats() {
        try {
            DashboardStatsDTO stats = predictionService.getDashboardStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Failed to fetch dashboard stats", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @PostMapping("/bulk-override")
    public ResponseEntity<BulkManualOverrideResponseDTO> applyBulkOverride(
            @RequestBody BulkManualOverrideRequestDTO request) {
        try {
            BulkManualOverrideResponseDTO response =
                    predictionService.applyBulkManualOverride(request);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error applying bulk override", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new BulkManualOverrideResponseDTO(
                            false, "Error: " + e.getMessage(), 0, 0, 0, List.of()
                    ));
        }
    }

    
}