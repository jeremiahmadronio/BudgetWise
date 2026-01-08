package com.example.budgetwise.prediction.repository;

import com.example.budgetwise.market.entity.MarketLocation;
import com.example.budgetwise.prediction.entity.PricePredictions;
import com.example.budgetwise.product.entity.ProductInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PricePredictionRepository extends JpaRepository<PricePredictions, Long>{


    @Query("""
        SELECT p FROM PricePredictions p 
        WHERE p.productInfo.id = :productId 
        AND p.targetDate = :targetDate 
        ORDER BY p.createdAt DESC
        LIMIT 1
    """)
    Optional<PricePredictions> findLatestPrediction(
            @Param("productId") Long productId,
            @Param("targetDate") LocalDate targetDate
    );

    Optional<PricePredictions> findByProductInfoAndMarketLocationAndTargetDate(
            ProductInfo product, MarketLocation market, LocalDate targetDate);

    @Query("""
        SELECT p FROM PricePredictions p 
        WHERE p.productInfo.id = :productId 
        AND p.marketLocation.id = :marketId 
        AND p.targetDate = :targetDate 
        ORDER BY p.createdAt DESC 
        LIMIT 1
    """)
    Optional<PricePredictions> findLatestPrediction(
            @Param("productId") Long productId,
            @Param("marketId") Long marketId,
            @Param("targetDate") LocalDate targetDate);

    // BATCH QUERY - kunin lahat ng existing predictions sa isang query
    @Query("""
        SELECT p FROM PricePredictions p 
        WHERE p.productInfo.id = :productId 
        AND p.marketLocation.id = :marketId 
        AND p.targetDate IN :targetDates
    """)
    List<PricePredictions> findByProductMarketAndDates(
            @Param("productId") Long productId,
            @Param("marketId") Long marketId,
            @Param("targetDates") List<LocalDate> targetDates);

    // BULK DELETE para sa cleanup
    @Modifying
    @Query("""
        DELETE FROM PricePredictions p 
        WHERE p.targetDate < :cutoffDate 
        AND p.status != 'OVERRIDDEN'
    """)
    int deleteOldPredictions(@Param("cutoffDate") LocalDate cutoffDate);



    @Query("""
    SELECT COUNT(p) FROM PricePredictions p 
    WHERE p.marketLocation.id = :marketId 
    AND p.targetDate = :targetDate
""")
    Integer countByMarketLocationIdAndTargetDate(
            @Param("marketId") Long marketId,
            @Param("targetDate") LocalDate targetDate
    );

    /**
     * Count anomalies in a market for a specific date
     */
    @Query("""
    SELECT COUNT(p) FROM PricePredictions p 
    WHERE p.marketLocation.id = :marketId 
    AND p.targetDate = :targetDate 
    AND p.status = :status
""")
    Integer countByMarketLocationIdAndTargetDateAndStatus(
            @Param("marketId") Long marketId,
            @Param("targetDate") LocalDate targetDate,
            @Param("status") PricePredictions.Status status
    );


    @Query("""
    SELECT COUNT(p) FROM PricePredictions p 
    WHERE p.targetDate = :targetDate
""")
    Integer countByTargetDate(@Param("targetDate") LocalDate targetDate);

    @Query("""
    SELECT COUNT(p) FROM PricePredictions p 
    WHERE p.targetDate = :targetDate 
    AND p.status = 'ANOMALY'
""")
    Integer countAnomaliesByDate(@Param("targetDate") LocalDate targetDate);

    @Query("""
    SELECT AVG(p.confidenceScore) FROM PricePredictions p 
    WHERE p.targetDate = :targetDate
""")
    Double calculateAverageConfidence(@Param("targetDate") LocalDate targetDate);

    @Query("""
    SELECT MAX(p.createdAt) FROM PricePredictions p
""")
    LocalDateTime findLatestPredictionTime();



    @Query("""
    SELECT p FROM PricePredictions p
    WHERE p.targetDate = :targetDate
    AND p.id IN (
        SELECT MAX(p2.id) 
        FROM PricePredictions p2
        WHERE p2.targetDate = :targetDate
        GROUP BY p2.productInfo.id, p2.marketLocation.id
    )
""")
    List<PricePredictions> findLatestPredictionsByDate(@Param("targetDate") LocalDate targetDate);

    /**
     * Count UNIQUE product-market pairs with predictions
     */
    @Query("""
    SELECT COUNT(DISTINCT CONCAT(p.productInfo.id, '-', p.marketLocation.id))
    FROM PricePredictions p 
    WHERE p.targetDate = :targetDate
""")
    Integer countUniquePredictionsByDate(@Param("targetDate") LocalDate targetDate);

    /**
     * Calculate average confidence from LATEST predictions only
     */
    @Query("""
    SELECT AVG(p.confidenceScore) 
    FROM PricePredictions p
    WHERE p.targetDate = :targetDate
    AND p.id IN (
        SELECT MAX(p2.id) 
        FROM PricePredictions p2
        WHERE p2.targetDate = :targetDate
        GROUP BY p2.productInfo.id, p2.marketLocation.id
    )
""")
    Double calculateAverageConfidenceFromLatest(@Param("targetDate") LocalDate targetDate);

    /**
     * Count anomalies from LATEST predictions only
     */
    @Query("""
    SELECT COUNT(p) 
    FROM PricePredictions p
    WHERE p.targetDate = :targetDate
    AND p.status = 'ANOMALY' 
    AND p.id IN (
        SELECT MAX(p2.id) 
        FROM PricePredictions p2
        WHERE p2.targetDate = :targetDate
        GROUP BY p2.productInfo.id, p2.marketLocation.id
    )
""")
    Integer countLatestAnomaliesByDate(@Param("targetDate") LocalDate targetDate);


    List<PricePredictions> findByProductInfoIdAndMarketLocationIdAndTargetDateAfter(
            Long productInfoId,
            Long marketLocationId,
            LocalDate targetDate
    );


   
}

