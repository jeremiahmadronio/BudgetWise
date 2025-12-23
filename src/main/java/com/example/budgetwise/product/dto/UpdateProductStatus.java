package com.example.budgetwise.product.dto;

public record UpdateProductStatus (

        Long id,
        String newStatus,
        String message
) {
}