package com.example.budgetwise.prediction.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ManualOverrideResponseDTO(
        boolean success,
        String message,
        Long predictionId,
        Double oldPrice,
        Double newPrice,
        String status
) {}