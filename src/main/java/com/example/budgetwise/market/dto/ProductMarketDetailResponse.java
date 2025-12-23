package com.example.budgetwise.market.dto;

import java.util.List;


public record ProductMarketDetailResponse (
        Long productId,
        String productName,
        List<MarketDetail> marketDetails
){

}