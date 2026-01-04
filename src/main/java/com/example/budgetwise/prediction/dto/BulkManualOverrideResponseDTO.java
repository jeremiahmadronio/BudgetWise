package com.example.budgetwise.prediction.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BulkManualOverrideResponseDTO(
        boolean success,
        String message,
        Integer totalProcessed,
        Integer successCount,
        Integer failedCount,
        List<OverrideResult> results
) {
    public record OverrideResult(
            Long productId,
            String productName,
            Long marketId,
            String marketName,
            boolean success,
            String message,
            Double oldPrice,
            Double newPrice,
            String status
    ) {}
}