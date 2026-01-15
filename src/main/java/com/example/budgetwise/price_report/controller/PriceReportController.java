package com.example.budgetwise.price_report.controller;

import com.example.budgetwise.price_report.dto.ReportTableResponse;
import com.example.budgetwise.price_report.service.PriceReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Controller
@RequestMapping("/api/v1/priceReport")
@RestController
@RequiredArgsConstructor
public class PriceReportController {

    private final PriceReportService priceReportService;

    @GetMapping("/table")
    public ResponseEntity<Page<ReportTableResponse>> getPriceReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(priceReportService.getPriceReports(
                page, size, status, source, startDate, endDate
        ));
    }
}
