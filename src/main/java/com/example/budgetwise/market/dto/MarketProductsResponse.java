package com.example.budgetwise.market.dto;

import com.example.budgetwise.market.entity.MarketLocation;

import java.time.LocalDate;

public record MarketProductsResponse(
        Long marketId,
        String marketName,
        MarketLocation.Type type,
        Long totalProducts,
        String productName,
        String productCategory,
        Double productPrice,
        LocalDate dateRecorded
) {
}