package com.example.budgetwise.market.dto;

public record ArchiveStatsResponse (
        long totalArchive,
        long archiveThisMonth,
        long highRated
) {
}
