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
            String url,
            LocalDate startDate,
            LocalDate endDate
    ) {
        Sort sort = Sort.by("dateReported").descending()
                .and(Sort.by("id").descending());

        Pageable pageable = PageRequest.of(page, size, sort);

        PriceReport.Status status = null;
        if (statusStr != null && !statusStr.isBlank()) {
            try {
                status = PriceReport.Status.valueOf(statusStr.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        String searchUrl = (url != null && !url.isBlank()) ? url : null;

        return priceReportRepository.findReportsWithStats(status, searchUrl, startDate, endDate, pageable);
    }
}
