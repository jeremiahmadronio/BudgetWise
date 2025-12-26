package com.example.budgetwise.analytics.repository.projection;

/**
 * Interface-based projection for cleaner analytics aggregation.
 * Spring Data JPA will automatically map query results to these methods.
 */
public interface SummaryStatsProjection {
    Double getMinPrice();
    Double getMaxPrice();
    Double getAvgPrice();
}