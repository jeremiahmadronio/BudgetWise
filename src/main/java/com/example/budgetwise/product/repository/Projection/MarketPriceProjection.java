package com.example.budgetwise.product.repository.Projection;

import java.sql.Timestamp;

public interface MarketPriceProjection {
    Long getMarketId();
    String getMarketName();
    String getMarketType();
    Timestamp getMarketOpeningTime(); 
    Timestamp getMarketClosingTime(); 
    Double getCurrentPrice();
    String getUnit();
}