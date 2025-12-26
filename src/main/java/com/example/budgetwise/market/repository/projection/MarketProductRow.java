package com.example.budgetwise.market.repository.projection;
import com.example.budgetwise.market.entity.MarketLocation;
import java.math.BigDecimal;
import java.time.LocalDate;
public interface MarketProductRow {
    Long getMarketId();
    String getMarketName();
    MarketLocation.Type getMarketType();
    String getProductName();
    String getProductCategory();
    BigDecimal getProductPrice();
    LocalDate getDateRecorded();
}
