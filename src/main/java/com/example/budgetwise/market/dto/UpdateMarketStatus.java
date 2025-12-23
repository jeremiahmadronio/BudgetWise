package com.example.budgetwise.market.dto;


import com.example.budgetwise.market.entity.MarketLocation;

public record UpdateMarketStatus (

        Long id,
        MarketLocation.Status newStatus

) {
}