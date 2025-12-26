package com.example.budgetwise.analytics.dto;

import com.example.budgetwise.market.entity.MarketLocation;

public record MarketLookup(Long id, String marketName, MarketLocation.Type type) {}