package com.example.budgetwise.market.dto;

import com.example.budgetwise.market.entity.MarketLocation;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MarketProductPriceView(
        Long marketId,
        String marketName,
        MarketLocation.Type type,
        Long totalProducts,
        String productName,
        String productCategory,
        BigDecimal productPrice,
        LocalDate dateRecorded
) {
}