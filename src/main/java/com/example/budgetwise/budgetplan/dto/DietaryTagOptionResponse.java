package com.example.budgetwise.budgetplan.dto;

import java.time.LocalDateTime;

public record DietaryTagOptionResponse(
        long id,
        String tagName,
        String description,
        LocalDateTime updatedAt
) {
}
