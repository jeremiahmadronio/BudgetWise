package com.example.budgetwise.market.service;

import com.example.budgetwise.market.dto.ArchiveStatsResponse;
import com.example.budgetwise.market.dto.BulkUpdateMarketStatus;
import com.example.budgetwise.market.dto.MarketArchiveTableResponse;
import com.example.budgetwise.market.entity.MarketLocation;
import com.example.budgetwise.market.repository.MarketLocationRepository;
import com.example.budgetwise.product.dto.BulkUpdateProductStatus;
import com.example.budgetwise.product.entity.ProductInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ArchiveMarketService {

    private final MarketLocationRepository marketLocationRepository;



    @Transactional(readOnly = true)
    public ArchiveStatsResponse getMarketArchiveStats() {
        long totalArchived = marketLocationRepository.countByStatus(
                MarketLocation.Status.INACTIVE
        );

        // Archived this month
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime startOfMonth = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = currentMonth.atEndOfMonth().atTime(LocalTime.MAX);

        long archivedThisMonth = marketLocationRepository.countByStatusAndUpdatedAtBetween(
                MarketLocation.Status.INACTIVE,
                startOfMonth,
                endOfMonth
        );

        // High rated (4.5 and above)
        long highRatedCount = marketLocationRepository.countByStatusAndRatingsGreaterThanEqual(
                MarketLocation.Status.INACTIVE,
                4.5
        );

        return new ArchiveStatsResponse(totalArchived, archivedThisMonth, highRatedCount);
    }



    @Transactional(readOnly = true)
    public Page<MarketArchiveTableResponse> getArchivedMarkets(String searchQuery, Pageable pageable) {
        MarketLocation.Status archivedStatus = MarketLocation.Status.INACTIVE;

        if (searchQuery != null && !searchQuery.isBlank()) {
            return marketLocationRepository.findArchivedMarketsWithSearch(
                    archivedStatus,
                    searchQuery,
                    pageable
            );
        } else {
            return marketLocationRepository.findArchivedMarketsNoSearch(
                    archivedStatus,
                    pageable
            );
        }
    }



    @Transactional
    public int updateMarketStatus(List<Long> marketIds, MarketLocation.Status newStatus) {
        // Validation
        if (marketIds == null || marketIds.isEmpty()) {
            throw new IllegalArgumentException("Market IDs list cannot be empty");
        }

        if (newStatus == null) {
            throw new IllegalArgumentException("New status cannot be null");
        }

        // Perform bulk update (works for 1 or many IDs)
        int updatedCount = marketLocationRepository.updateStatusForIds(newStatus, marketIds);

        if (updatedCount == 0) {
            throw new RuntimeException("No markets were updated. Please check if the IDs exist.");
        }

        return updatedCount;
    }

    /**
     * Convenience method: Archive markets (single or bulk)
     */
    @Transactional
    public int archiveMarkets(List<Long> marketIds) {
        return updateMarketStatus(marketIds, MarketLocation.Status.INACTIVE);
    }





}
