package com.example.budgetwise.prediction.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BulkManualOverrideRequestDTO(
        // OPTION 1: Single override
        Long productId,           // Single product
        Long marketId,            // Single market

        // OPTION 2: Bulk override by product (all markets)
        List<Long> productIds,    // Multiple products

        // OPTION 3: Bulk override by market (all products)
        List<Long> marketIds,     // Multiple markets

        // OPTION 4: Specific pairs
        List<ProductMarketPair> pairs,  // Specific product-market combinations

        // Override settings (applies to all)
        LocalDate targetDate,
        String forceTrend,        // "+10% Increase", "-20% Decrease", etc.
        Double manualPrice,       // Or direct price (overrides forceTrend)
        String reason,

        // Bulk options
        Boolean overrideAllMarkets,  // If true, apply to ALL markets for given products
        Boolean overrideAllProducts  // If true, apply to ALL products for given markets
) {
    public record ProductMarketPair(Long productId, Long marketId) {}

    public boolean isValid() {
        // Must have at least one target
        boolean hasTarget = productId != null ||
                (productIds != null && !productIds.isEmpty()) ||
                (marketIds != null && !marketIds.isEmpty()) ||
                (pairs != null && !pairs.isEmpty());

        // Must have override method
        boolean hasOverride = forceTrend != null || manualPrice != null;

        return hasTarget && hasOverride && reason != null && !reason.trim().isEmpty();
    }
}