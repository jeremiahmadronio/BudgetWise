package com.example.budgetwise.budgetplan.controller;

import com.example.budgetwise.budgetplan.dto.DietaryArchiveResponse;
import com.example.budgetwise.budgetplan.dto.DietaryArchiveStatsResponse;
import com.example.budgetwise.budgetplan.entity.DietaryTag;
import com.example.budgetwise.budgetplan.service.DietaryTagArchiveService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@Controller
@RequestMapping("/api/v1/archiveTag")
@RequiredArgsConstructor
public class ArchiveController {


    private final DietaryTagArchiveService  dietaryTagArchiveService;


    @GetMapping("/stats")
    public ResponseEntity<DietaryArchiveStatsResponse> getArchiveStats() {
        return ResponseEntity.ok(dietaryTagArchiveService.getDietaryArchiveStats());
    }

    // GET PAGINATED TABLE
    @GetMapping("/archive")
    public ResponseEntity<Page<DietaryArchiveResponse>> getArchivedTags(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(dietaryTagArchiveService.getArchivedTags(query, pageable));
    }

    // BULK RESTORE / ARCHIVE
    @PutMapping("/status")
    public ResponseEntity<Map<String, Object>> updateTagStatus(
            @RequestBody List<Long> ids,
            @RequestParam DietaryTag.Status status
    ) {
        int count = dietaryTagArchiveService.updateDietaryTagStatus(ids, status);
        return ResponseEntity.ok(Map.of(
                "message", "Successfully updated " + count + " tags to " + status,
                "count", count
        ));
    }
}
