package com.example.budgetwise.price_report.dto;

import com.example.budgetwise.price_report.entity.PriceReport;

import java.time.LocalDate;

public record ReportTableResponse(
        long id,
        LocalDate dateReported,
        long totalProducts,
        long totalMarkets,
        long durationMs,
        PriceReport.DataSource source,
        PriceReport.Status status


) {
}
