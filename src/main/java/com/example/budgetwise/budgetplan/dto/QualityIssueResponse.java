package com.example.budgetwise.budgetplan.dto;

import java.time.LocalDateTime;
import java.util.List;

public record QualityIssueResponse(
        Long productId,
        String productName,
        String category,
        String issueType,
        String severity,
        String description,
        String suggestedFix,
        List<String> currentTags,

        LocalDateTime lastUpdated
) {
}
