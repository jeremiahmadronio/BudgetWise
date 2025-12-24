package com.example.budgetwise.product.dto;

import com.example.budgetwise.product.entity.ProductInfo;

public record ProductUpdateRequest(
        Long id,
        String localName,
        double price,
        String unit,
        ProductInfo.Status status
) {
}
