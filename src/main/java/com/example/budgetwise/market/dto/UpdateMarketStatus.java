package com.example.budgetwise.market.dto;


import com.example.budgetwise.market.entity.MarketLocation;

import java.util.List;

public record UpdateMarketStatus (

        List<Long> ids,
        MarketLocation.Status newStatus

) {
}