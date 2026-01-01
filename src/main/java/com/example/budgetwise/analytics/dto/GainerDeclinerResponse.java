package com.example.budgetwise.analytics.dto;

import java.util.List;

public record GainerDeclinerResponse(
        List<PriceMovement> topGainers,
        List<PriceMovement> topDecliners,
        int allGainersCount,
        int allDeclinersCount

) {}