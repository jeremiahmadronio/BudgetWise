package com.example.budgetwise.budgetplan.dto;

import java.util.List;

public record UpdateProductTagsRequest(
        List<Long> tagIds
) {
}
