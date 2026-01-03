package com.example.budgetwise.prediction;

import java.time.LocalDate;

public record PredictionDTO(
        Long id,
        Double predictedPrice,
        Double confidenceScore,
        String status,
        LocalDate targetDate
) {}