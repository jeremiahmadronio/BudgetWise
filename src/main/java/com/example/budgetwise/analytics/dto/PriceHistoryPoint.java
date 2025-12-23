package com.example.budgetwise.analytics.dto;
import java.time.LocalDate;

public record PriceHistoryPoint(
        LocalDate date,
        Double price
) {}
