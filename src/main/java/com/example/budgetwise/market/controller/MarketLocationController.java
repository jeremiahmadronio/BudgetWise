package com.example.budgetwise.market.controller;



import com.example.budgetwise.market.dto.*;
import com.example.budgetwise.market.entity.MarketLocation;
import com.example.budgetwise.market.service.MarketLocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/markets")
@RequiredArgsConstructor
public class MarketLocationController {

    private final MarketLocationService marketLocationService;


    @GetMapping("/stats")
    public ResponseEntity<MarketStatsResponse> getMarketStats() {
        MarketStatsResponse response = marketLocationService.getMarketStats();
        return ResponseEntity.ok(response);
    }


    @GetMapping("/displayMarkets")
    public ResponseEntity<Page<MarketTableResponse>>displayMarkets(
            @PageableDefault(size = 10, sort = "marketLocation", direction = Sort.Direction.ASC) Pageable pageable
    ){
        Page<MarketTableResponse> response = marketLocationService.displayMarketTableInfo(pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/market-products/{marketId}")
    public ResponseEntity<List<MarketProductPriceView>> getMarketProducts(@PathVariable Long marketId) {
        List<MarketProductPriceView> response = marketLocationService.getMarketProducts(marketId);

        if (response.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/bulk-status")
    public ResponseEntity<String> updateMarketStatuses(@RequestBody UpdateMarketStatus request) {
        marketLocationService.updateMarketStatuses(request);
        return ResponseEntity.ok("Successfully updated status for " + request.ids().size() + " market(s).");
    }


    @PostMapping("addMarket")
    public ResponseEntity<MarketLocation> createMarket(
            @RequestBody @Valid CreateMarket request) {

        MarketLocation createdMarket = marketLocationService.addMarket(request);

        // Return 201 Created status + the created object
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdMarket);
    }
    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id, @Valid @RequestBody UpdateMarket request) {
        marketLocationService.updateMarket(id, request);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves detailed information for a single market location.
     * PERFORMANCE STRATEGY:
     * 1. Constructor Projection: Uses JPQL 'new' expression to map database results directly
     * to MarketViewResponse. This bypasses the Hibernate Persistence Context (L1 Cache),
     * reducing memory overhead.
     * 2. Optimized Aggregation: Includes a Scalar Subquery to count unique products per market
     * directly at the database level, avoiding the need to load large collection associations.
     * 3. Read-Only Transaction: Annotated with (readOnly = true) to allow the database to
     * optimize for concurrent read performance.
     *
     * @param id The unique identifier of the market.
     * @return A {@link MarketViewResponse} containing market details and product statistics.
     * @throws RuntimeException if no market is found with the specified ID.
     */
    @GetMapping("/view/{id}")
    public ResponseEntity<MarketViewResponse> getMarketDetails(@PathVariable Long id) {
        return ResponseEntity.ok(marketLocationService.getMarketById(id));
    }
}