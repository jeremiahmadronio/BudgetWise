package com.example.budgetwise.prediction.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Request DTO for product-market pair operations
 * Using Java record for immutability and conciseness
 */
public record ProductMarketPairRequest(
        @NotNull(message = "Product ID is required")
        @Positive(message = "Product ID must be positive")
        Long productId,

        @NotNull(message = "Market ID is required")
        @Positive(message = "Market ID must be positive")
        Long marketId
) {
    // Compact constructor for additional validation if needed
    public ProductMarketPairRequest {
        if (productId != null && productId <= 0) {
            throw new IllegalArgumentException("Product ID must be positive");
        }
        if (marketId != null && marketId <= 0) {
            throw new IllegalArgumentException("Market ID must be positive");
        }
    }
}