package com.example.budgetwise.prediction.dto;

public record PriceCalibrationDTO(
        Long productId,
        String productName,
        Double currentPrice,
        Double forecastPrice,
        Double trendPercentage, // Computed field
        Double confidenceScore,
        String status
){
}
