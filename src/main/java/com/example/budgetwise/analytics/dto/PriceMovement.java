package com.example.budgetwise.analytics.dto;

public record PriceMovement(
        String productName,
        Double currentPrice,
        Double oldPrice,
        Double percentageChange,
        String trend // "UP" or "DOWN"
) {}