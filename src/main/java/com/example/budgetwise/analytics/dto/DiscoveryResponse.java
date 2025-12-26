package com.example.budgetwise.analytics.dto;

import java.util.List;

public record DiscoveryResponse(
        List<MarketLookup> markets,
        List<ProductLookup> products
) {}