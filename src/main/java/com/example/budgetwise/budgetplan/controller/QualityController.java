package com.example.budgetwise.budgetplan.controller;


import com.example.budgetwise.budgetplan.dto.QualityIssueResponse;
import com.example.budgetwise.budgetplan.service.QualityManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/quality") // Base Path
@RequiredArgsConstructor
public class QualityController {

    private final QualityManagementService qualityManagementService;


    @GetMapping("/scan")
    public ResponseEntity<List<QualityIssueResponse>> scanQualityIssues() {

        List<QualityIssueResponse> issues = qualityManagementService.scanForQualityIssues();

        return ResponseEntity.ok(issues);
    }
}