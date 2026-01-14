package com.example.budgetwise.budgetplan.dto;

public record DietaryStatsResponse(
        long totalProducts,
        long taggedProducts,
        long untaggedProducts,
        long totalDietaryOption
) {
}
