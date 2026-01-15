package com.example.budgetwise.budgetplan.dto;

public record CategoryCoverageResponse(
        String category,
        long taggedCount,
        long totalCount,
        double coveragePercentage,
        String status

) {
}
