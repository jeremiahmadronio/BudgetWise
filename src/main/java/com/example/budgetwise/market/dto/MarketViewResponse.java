package com.example.budgetwise.market.dto;

import com.example.budgetwise.market.entity.MarketLocation;

import java.time.LocalDateTime;

public record MarketViewResponse(

        Long id,
        String marketName,
        MarketLocation.Type type,
        MarketLocation.Status status,
        double latitude,
        double longitude,
        Long totalProducts,
        LocalDateTime openingTime,
        LocalDateTime closingTime,
        double ratings,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
