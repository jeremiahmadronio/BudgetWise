package com.example.budgetwise.analytics.dto;

public record MarketComparisonChart(
        String marketName,
        Double averagePrice,
        boolean isTargetMarket
) {
}
