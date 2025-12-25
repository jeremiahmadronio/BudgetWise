package com.example.budgetwise.market.dto;


import com.example.budgetwise.market.entity.MarketLocation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record UpdateMarket(
        @NotBlank(message = "Market location name is required")
        String marketLocation,

        @NotNull(message = "Type is required")
        MarketLocation.Type type,

        @NotNull(message = "Status is required")
        MarketLocation.Status status,

        @NotNull(message = "Latitude is required")
        Double latitude,

        @NotNull(message = "Longitude is required")
        Double longitude,

        @NotNull(message = "Ratings is required")
        Double ratings,

        LocalDateTime openingTime,
        LocalDateTime closingTime,

        String description
) {}