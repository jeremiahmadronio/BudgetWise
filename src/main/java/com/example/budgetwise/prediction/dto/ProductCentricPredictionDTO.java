package com.example.budgetwise.prediction.dto;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProductCentricPredictionDTO(
        Long productId,
        String productName,
        String productCode,
        String category,
        List<MarketPrediction> marketPredictions,
        Double averageCurrentPrice,
        Double averageForecastPrice,
        Double maxPriceDifference,
        String mostExpensiveMarket,
        String cheapestMarket,
        Integer totalMarkets,
        Integer anomalyCount
) {

    /**
     * Nested record for individual market prediction
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record MarketPrediction(
            Long marketId,
            String marketName,
            String marketLocation,
            Double currentPrice,
            Double forecastPrice,
            Double trendPercentage,
            Double confidenceScore,
            String status,
            Integer dataPoints
    ) {
        public String getConfidenceLevel() {
            if (confidenceScore == null) return "UNKNOWN";
            if (confidenceScore > 0.7) return "HIGH";
            if (confidenceScore > 0.4) return "MEDIUM";
            return "LOW";
        }

        public String getTrendDirection() {
            if (trendPercentage == null) return "STABLE";
            if (Math.abs(trendPercentage) < 0.5) return "STABLE";
            return trendPercentage > 0 ? "UP" : "DOWN";
        }
    }
}