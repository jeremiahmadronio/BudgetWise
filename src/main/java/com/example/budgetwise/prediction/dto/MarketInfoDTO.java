package com.example.budgetwise.prediction.dto;

public record MarketInfoDTO(
        Long id,
        String name,
        String location,
        Integer productCount,
        Integer predictionCount,
        Integer anomalyCount
) {}