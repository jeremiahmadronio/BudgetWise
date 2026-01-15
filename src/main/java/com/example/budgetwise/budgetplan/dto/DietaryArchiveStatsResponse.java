package com.example.budgetwise.budgetplan.dto;

public record DietaryArchiveStatsResponse(
        long totalArchived,
        long archivedThisMonth,
        long unusedTagsCount
) {
}
