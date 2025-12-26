package com.example.budgetwise.analytics.repository;



import com.example.budgetwise.analytics.dto.PriceHistoryPoint;
import com.example.budgetwise.analytics.repository.projection.SummaryStatsProjection;
import com.example.budgetwise.product.entity.DailyPriceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Read-Only Repository for Analytics.
 * * Utilizes custom JPQL queries to:
 * 1. Efficiently calculate AVG, MIN, and MAX at the database level.
 * 2. Avoid fetching full Entity objects (uses DTO projections).
 */
public interface AnalyticsRepository extends JpaRepository<DailyPriceRecord, Long> {


    @Query("""
        SELECT new com.example.budgetwise.analytics.dto.PriceHistoryPoint(
            dpr.priceReport.dateReported,
            dpr.price
        )
        FROM DailyPriceRecord dpr
        WHERE dpr.productInfo.productName = :productName
          AND dpr.marketLocation.id = :marketId
          AND dpr.priceReport.dateReported >= :startDate
        ORDER BY dpr.priceReport.dateReported ASC
    """)
    List<PriceHistoryPoint> findHistoryByMarket(
            @Param("productName") String productName,
            @Param("marketId") Long marketId,
            @Param("startDate") LocalDate startDate
    );

    @Query("""
        SELECT new com.example.budgetwise.analytics.dto.PriceHistoryPoint(
            dpr.priceReport.dateReported,
            AVG(dpr.price)
        )
        FROM DailyPriceRecord dpr
        WHERE dpr.productInfo.productName = :productName
          AND dpr.priceReport.dateReported >= :startDate
        GROUP BY dpr.priceReport.dateReported
        ORDER BY dpr.priceReport.dateReported ASC
    """)
    List<PriceHistoryPoint> findHistoryNationalAverage(
            @Param("productName") String productName,
            @Param("startDate") LocalDate startDate
    );


    @Query("""
        SELECT MIN(dpr.price) AS minPrice, 
               MAX(dpr.price) AS maxPrice, 
               AVG(dpr.price) AS avgPrice
        FROM DailyPriceRecord dpr
        WHERE dpr.productInfo.productName = :productName
          AND dpr.marketLocation.id = :marketId
          AND dpr.priceReport.dateReported >= :startDate
    """)
    Optional<SummaryStatsProjection> findStatsByMarket(
            @Param("productName") String productName,
            @Param("marketId") Long marketId,
            @Param("startDate") LocalDate startDate
    );

    @Query("""
        SELECT MIN(dpr.price) AS minPrice, 
               MAX(dpr.price) AS maxPrice, 
               AVG(dpr.price) AS avgPrice
        FROM DailyPriceRecord dpr
        WHERE dpr.productInfo.productName = :productName
          AND dpr.priceReport.dateReported >= :startDate
    """)
    Optional<SummaryStatsProjection> findStatsNational(
            @Param("productName") String productName,
            @Param("startDate") LocalDate startDate
    );
}