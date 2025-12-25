package com.example.budgetwise.product.repository;


import com.example.budgetwise.product.entity.DailyPriceRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface DailyPriceRecordRepository extends JpaRepository<DailyPriceRecord, Long> {

    /**
     * Projection interface to map the result of the count query.
     */
    public interface MarketCountProjection{
        Long getProductId();
        Long getTotalMarkets();
    }

    interface PriceProjection {
        Long getProductId();
        Double getPrice();
    }


    @Query("""
    SELECT dpr.productInfo.id AS productId, COUNT(DISTINCT dpr.marketLocation.id) AS totalMarkets
    FROM DailyPriceRecord dpr
    WHERE dpr.productInfo.id IN :productIds
    AND dpr.priceReport.id = (SELECT MAX(r.id) FROM PriceReport r WHERE r.status = 'COMPLETED')
    GROUP BY dpr.productInfo.id
""")
    List<MarketCountProjection> countCurrentMarketsByProductIds(@Param("productIds") List<Long> productIds);


    @Query(value = """
    WITH RankedPrices AS (
        SELECT 
            dpr.product_info_id as productId, 
            dpr.price as price,
            DENSE_RANK() OVER (
                PARTITION BY dpr.product_info_id 
                ORDER BY pr.date_reported DESC
            ) as report_rank
        FROM daily_price_record dpr
        JOIN price_report pr ON dpr.price_report_id = pr.id
        WHERE dpr.product_info_id IN :productIds
        AND pr.status = 'COMPLETED'
    )
    SELECT productId, MAX(price) as price
    FROM RankedPrices
    WHERE report_rank = 2
    GROUP BY productId
""", nativeQuery = true)
    List<PriceProjection> findPreviousPricesByProductIds(@Param("productIds") List<Long> productIds);
    @Query("""
           SELECT d FROM DailyPriceRecord d 
                      JOIN d.priceReport r 
                      WHERE d.productInfo.id = :productId 
                                 ORDER BY r.dateReported DESC, d.createdAt DESC
           """)
    List<DailyPriceRecord> findLatestByProductId(@Param("productId") Long productId, Pageable pageable);

}