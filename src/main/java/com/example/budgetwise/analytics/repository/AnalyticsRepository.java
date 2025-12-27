package com.example.budgetwise.analytics.repository;

import com.example.budgetwise.analytics.dto.MarketComparisonChart;
import com.example.budgetwise.analytics.dto.PriceHistoryPoint;
import com.example.budgetwise.analytics.repository.projection.SummaryStatsProjection;
import com.example.budgetwise.product.entity.DailyPriceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AnalyticsRepository extends JpaRepository<DailyPriceRecord, Long> {

    /**
     * Finds the average price reported before the chart window starts.
     * This prevents the chart from starting at 0.0.
     */
    @Query("""
        SELECT AVG(dpr.price) FROM DailyPriceRecord dpr 
        WHERE LOWER(TRIM(dpr.productInfo.productName)) = LOWER(TRIM(:productName)) 
        AND (:marketId IS NULL OR dpr.marketLocation.id = :marketId)
        AND dpr.priceReport.dateReported < :startDate
        AND dpr.priceReport.status = 'COMPLETED'
    """)
    Optional<Double> findLatestPriceBefore(
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
        WHERE LOWER(TRIM(dpr.productInfo.productName)) = LOWER(TRIM(:productName))
          AND dpr.marketLocation.id = :marketId
          AND dpr.priceReport.dateReported >= :startDate
          AND dpr.priceReport.status = 'COMPLETED'
        GROUP BY dpr.priceReport.dateReported
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
        WHERE LOWER(TRIM(dpr.productInfo.productName)) = LOWER(TRIM(:productName))
          AND dpr.priceReport.dateReported >= :startDate
          AND dpr.priceReport.status = 'COMPLETED'
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
        WHERE LOWER(TRIM(dpr.productInfo.productName)) = LOWER(TRIM(:productName))
          AND (:marketId IS NULL OR dpr.marketLocation.id = :marketId)
          AND dpr.priceReport.dateReported >= :startDate
          AND dpr.priceReport.status = 'COMPLETED'
    """)
    Optional<SummaryStatsProjection> findCombinedStats(
            @Param("productName") String productName,
            @Param("marketId") Long marketId,
            @Param("startDate") LocalDate startDate
    );



    @Query("""
        SELECT new com.example.budgetwise.analytics.dto.MarketComparisonChart(
            m.marketLocation,
            AVG(dpr.price),
            (m.id = :targetMarketId)
        )
        FROM DailyPriceRecord dpr
        JOIN dpr.marketLocation m
        WHERE LOWER(TRIM(dpr.productInfo.productName)) = LOWER(TRIM(:productName))
          AND dpr.priceReport.dateReported >= :startDate
          AND dpr.priceReport.status = 'COMPLETED'
        GROUP BY m.id, m.marketLocation
        ORDER BY AVG(dpr.price) ASC
    """)
    List<MarketComparisonChart> findMarketComparisonData(
            @Param("productName") String productName,
            @Param("targetMarketId") Long targetMarketId,
            @Param("startDate") LocalDate startDate
    );
}