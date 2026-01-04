package com.example.budgetwise.prediction.repository;

import com.example.budgetwise.product.entity.DailyPriceRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DailyPriceRecordPredictionRepository extends JpaRepository<DailyPriceRecord, Long> {

    @Query("""
           SELECT d FROM DailyPriceRecord d 
                      JOIN d.priceReport r 
                      WHERE d.productInfo.id = :productId 
                                 ORDER BY r.dateReported DESC, d.createdAt DESC
           """)
    List<DailyPriceRecord> findLatestByProductId(@Param("productId") Long productId, Pageable pageable);


    @Query("""
        SELECT d FROM DailyPriceRecord d
        JOIN FETCH d.priceReport pr
        JOIN FETCH d.productInfo pi
        JOIN FETCH d.marketLocation ml
        WHERE pi.id = :productId 
        AND ml.id = :marketId
        ORDER BY pr.dateReported DESC
        LIMIT 30
    """)
    List<DailyPriceRecord> findTop30ByProductInfoIdAndMarketLocationIdOrderByPriceReport_DateReportedDesc(
            @Param("productId") Long productId,
            @Param("marketId") Long marketId
    );



    @Query("""
        SELECT DISTINCT d.productInfo.id, d.marketLocation.id 
        FROM DailyPriceRecord d
        WHERE d.productInfo.status = 'ACTIVE'
        ORDER BY d.productInfo.id, d.marketLocation.id
    """)
    List<Object[]> findExistingProductMarketPairs();

    @Query("""
        SELECT d.price 
        FROM DailyPriceRecord d
        JOIN d.priceReport pr
        WHERE d.productInfo.id = :productId 
        AND d.marketLocation.id = :marketId
        ORDER BY pr.dateReported DESC
        LIMIT 1
    """)
    Optional<Double> findLatestPriceByProductAndMarket(
            @Param("productId") Long productId,
            @Param("marketId") Long marketId
    );

    @Query("""
        SELECT d.productInfo.id, MAX(d.price) 
        FROM DailyPriceRecord d 
        WHERE d.productInfo.id IN :productIds 
        AND d.marketLocation.id = :marketId
        GROUP BY d.productInfo.id
    """)
    List<Object[]> findLatestPricesByProductsAndMarket(
            @Param("productIds") List<Long> productIds,
            @Param("marketId") Long marketId
    );



    /**
     * Count distinct products that have prices in a market
     */
    @Query("""
    SELECT COUNT(DISTINCT dpr.productInfo.id) 
    FROM DailyPriceRecord dpr 
    WHERE dpr.marketLocation.id = :marketId
""")
    Integer countDistinctProductsByMarketId(@Param("marketId") Long marketId);

    /**
     * Count price records for a product-market pair
     */
    @Query("""
    SELECT COUNT(dpr) FROM DailyPriceRecord dpr 
    WHERE dpr.productInfo.id = :productId 
    AND dpr.marketLocation.id = :marketId
""")
    Integer countByProductInfoIdAndMarketLocationId(
            @Param("productId") Long productId,
            @Param("marketId") Long marketId
    );
}
