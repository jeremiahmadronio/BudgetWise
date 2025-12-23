package com.example.budgetwise.product.dto;

public record ArchiveStatsResponse(
        long totalArchived,
        long newThisMonth,
        long awaitingReview
) {
}