package com.example.budgetwise.market.dto;

import com.example.budgetwise.market.entity.MarketLocation;

import java.time.LocalDateTime;

public record MarketArchiveTableResponse (
         Long id,
         String marketLocation,
         MarketLocation.Type type,
         double ratings,
         LocalDateTime archivedDate
) {
}
