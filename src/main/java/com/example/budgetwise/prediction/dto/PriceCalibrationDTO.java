package com.example.budgetwise.prediction.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PriceCalibrationDTO(
        Long productId,
        String productName,
        Long marketId,
        String marketName,
        Double currentPrice,
        Double forecastPrice,
        Double trendPercentage,
        Double confidenceScore,
        String status
) {

    public String getConfidenceLevel() {
        if (confidenceScore == null) return "UNKNOWN";

        double percentage = confidenceScore * 100;

        if (percentage >= 70) return "HIGH";
        if (percentage >= 50) return "MEDIUM";
        if (percentage >= 30) return "LOW";
        return "VERY_LOW";
    }

    public String getConfidenceLabel() {
        if (confidenceScore == null) return "No Data";

        double percentage = confidenceScore * 100;

        if (percentage >= 70) return String.format("%.0f%% High", percentage);
        if (percentage >= 50) return String.format("%.0f%% Medium", percentage);
        if (percentage >= 30) return String.format("%.0f%% Low", percentage);
        return String.format("%.0f%% Very Low", percentage);
    }

    public String getStatusColor() {
        if (status == null) return "gray";
        return switch (status) {
            case "NORMAL" -> "green";
            case "ANOMALY" -> "red";
            case "OVERRIDDEN" -> "yellow";
            default -> "gray";
        };
    }


   
}
