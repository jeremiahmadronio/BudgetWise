package com.example.budgetwise.market.dto;

import com.example.budgetwise.market.entity.MarketLocation;

import java.time.LocalDateTime;

public record MarketDetail(
        Long marketId,
        String marketName,
        MarketLocation.Type marketType,
        LocalDateTime marketOpeningTime,
        LocalDateTime marketClosingTime
){
}