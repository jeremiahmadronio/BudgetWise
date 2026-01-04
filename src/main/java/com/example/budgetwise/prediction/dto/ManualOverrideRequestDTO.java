package com.example.budgetwise.prediction.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ManualOverrideRequestDTO(
        Long productId,
        Long marketId,
        LocalDate targetDate,
        String forceTrend,      // "NO_OVERRIDE", "+10% Increase", "-20% Decrease", "STABILIZE"
        Double manualPrice,    
        String reason           
) {
    // Validation
    public boolean isValid() {
        return productId != null && marketId != null &&
                (forceTrend != null || manualPrice != null);
    }
}