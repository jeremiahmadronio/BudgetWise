package com.example.budgetwise.market.dto;

import com.example.budgetwise.market.entity.MarketLocation;

public record MarketTableResponse (

        Long id,
        String marketName,
        MarketLocation.Type marketType,
        MarketLocation.Status marketStatus,
        Long totalProductsAvailable

){
}