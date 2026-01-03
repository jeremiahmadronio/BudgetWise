package com.example.budgetwise.prediction;

import com.example.budgetwise.prediction.dto.PriceCalibrationDTO;
import com.example.budgetwise.prediction.service.PricePredictionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/predictions")
@RequiredArgsConstructor
public class PricePredictionController {

    private final PricePredictionService predictionService;


    @PostMapping("/bulk-trigger")
    public ResponseEntity<String> triggerBulk() {
        predictionService.runBulkPrediction();
        return ResponseEntity.ok("Bulk market-aware prediction triggered successfully.");
    }

    @GetMapping("/calibration-table/{marketId}")
    public ResponseEntity<Page<PriceCalibrationDTO>> getTable(
            @PathVariable Long marketId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "7") int size) {

        return ResponseEntity.ok(predictionService.getCalibrationTable(marketId, PageRequest.of(page, size)));
    }
}