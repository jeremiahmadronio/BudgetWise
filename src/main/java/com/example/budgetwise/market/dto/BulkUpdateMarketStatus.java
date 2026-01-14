package com.example.budgetwise.market.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

@NotNull
public record BulkUpdateMarketStatus (
        List<Long> ids,
        String newStatus
) {
}
