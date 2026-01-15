package com.example.budgetwise.budgetplan.dto;

import java.time.LocalDateTime;

public record DietaryArchiveResponse(
        Long id,
        String tagName,
        String description,
        LocalDateTime archivedAt
) {
}
