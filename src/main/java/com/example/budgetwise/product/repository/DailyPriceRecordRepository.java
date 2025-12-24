package com.example.budgetwise.product.repository;


import com.example.budgetwise.product.entity.DailyPriceRecord;
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


    @Query("""
    SELECT dpr.productInfo.id AS productId, COUNT(DISTINCT dpr.marketLocation.id) AS totalMarkets
    FROM DailyPriceRecord dpr
    WHERE dpr.productInfo.id IN :productIds
    AND dpr.priceReport.id = (SELECT MAX(r.id) FROM PriceReport r WHERE r.status = 'COMPLETED')
    GROUP BY dpr.productInfo.id
""")
    List<MarketCountProjection> countCurrentMarketsByProductIds(@Param("productIds") List<Long> productIds);


}