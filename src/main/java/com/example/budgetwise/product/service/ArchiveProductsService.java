package com.example.budgetwise.product.service;

import com.example.budgetwise.market.repository.MarketLocationRepository;
import com.example.budgetwise.product.dto.ArchiveStatsResponse;
import com.example.budgetwise.product.dto.ArchiveTableResponse;
import com.example.budgetwise.product.entity.ProductInfo;
import com.example.budgetwise.product.repository.DailyPriceRecordRepository;
import com.example.budgetwise.product.repository.ProductInfoRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ArchiveProductsService {
    
    private final ProductInfoRepository productInfoRepository;
    private final DailyPriceRecordRepository dailyPriceRecordRepository;
    private final MarketLocationRepository marketLocationRepository;




    @Transactional(readOnly = true)
    public ArchiveStatsResponse getArchiveStats() {
        long inactiveCount = productInfoRepository.countByStatus(ProductInfo.Status.INACTIVE);
        long pendingCount = productInfoRepository.countByStatus(ProductInfo.Status.PENDING);
        long totalArchived = inactiveCount + pendingCount;

        YearMonth currentMonth = YearMonth.now();
        LocalDateTime startOfMonth = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = currentMonth.atEndOfMonth().atTime(LocalTime.MAX);


        long archivedThisMonth = productInfoRepository.countByStatusInAndUpdatedAtBetween(
                Arrays.asList(ProductInfo.Status.INACTIVE, ProductInfo.Status.PENDING),
                startOfMonth,
                endOfMonth
        );


        return new ArchiveStatsResponse(totalArchived, archivedThisMonth, pendingCount);
    }


    @Transactional(readOnly = true)
    public Page<ArchiveTableResponse> getArchivedProducts(String searchQuery, Pageable pageable) {

        // Target: INACTIVE at PENDING
        List<ProductInfo.Status> archivedStatuses = Arrays.asList(
                ProductInfo.Status.INACTIVE,
                ProductInfo.Status.PENDING
        );

        if (searchQuery != null && !searchQuery.isBlank()) {
            return productInfoRepository.findArchivedProductsWithSearch(
                    archivedStatuses, searchQuery, pageable);
        } else {
            return productInfoRepository.findArchivedProductsNoSearch(
                    archivedStatuses, pageable);
        }
    }


}
