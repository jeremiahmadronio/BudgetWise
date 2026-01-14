package com.example.budgetwise.market.controller;

import com.example.budgetwise.market.dto.ArchiveStatsResponse;
import com.example.budgetwise.market.dto.MarketArchiveTableResponse;
import com.example.budgetwise.market.entity.MarketLocation;
import com.example.budgetwise.market.service.ArchiveMarketService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RestController
@RequestMapping("api/v1/markets/archive")
@RequiredArgsConstructor
public class MarketArchiveController {

    private final ArchiveMarketService archiveMarketService;



    /**
     * Get archive statistics (for dashboard cards)
     * GET /api/markets/archive/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<ArchiveStatsResponse> getArchiveStats() {
        ArchiveStatsResponse stats = archiveMarketService.getMarketArchiveStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * Get archived markets table with pagination and search
     * GET /api/markets/archive?search=farmers&page=0&size=10
     */
    @GetMapping("/table")
    public ResponseEntity<Page<MarketArchiveTableResponse>> getArchivedMarkets(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<MarketArchiveTableResponse> markets = archiveMarketService.getArchivedMarkets(search, pageable);
        return ResponseEntity.ok(markets);
    }



    /**
     * Restore markets (single or bulk)
     * POST /api/markets/archive/restore
     * Body: [1, 2, 3]
     */
    @PostMapping("/restore")
    public ResponseEntity<Map<String, Object>> restoreMarkets(
            @RequestBody List<Long> marketIds
    ) {
        int restoredCount = archiveMarketService.updateMarketStatus(
                marketIds,
                MarketLocation.Status.ACTIVE
        );

        return ResponseEntity.ok(Map.of(
                "message", "Market(s) restored successfully",
                "restoredCount", restoredCount
        ));
    }


}
