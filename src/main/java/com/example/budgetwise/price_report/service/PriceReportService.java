package com.example.budgetwise.price_report.service;

import com.example.budgetwise.price_report.dto.ReportTableResponse;
import com.example.budgetwise.price_report.entity.PriceReport;
import com.example.budgetwise.price_report.repository.PriceReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class PriceReportService {

    private final PriceReportRepository priceReportRepository;


    @Transactional(readOnly = true)
    public Page<ReportTableResponse> getPriceReports(
            int page,
            int size,
            String statusStr,
            String sourceStr,
            LocalDate startDate,
            LocalDate endDate
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("dateReported").descending());

        PriceReport.Status status = null;
        if (statusStr != null && !statusStr.isBlank()) {
            try {
                status = PriceReport.Status.valueOf(statusStr.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        PriceReport.DataSource source = null;
        if (sourceStr != null && !sourceStr.isBlank()) {
            try {
                source = PriceReport.DataSource.valueOf(sourceStr.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }
        return priceReportRepository.findReportsWithStats(status, source, startDate, endDate, pageable);
    }
}
