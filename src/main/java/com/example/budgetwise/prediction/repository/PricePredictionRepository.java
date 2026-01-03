package com.example.budgetwise.prediction.repository;

import com.example.budgetwise.market.entity.MarketLocation;
import com.example.budgetwise.prediction.entity.PricePredictions;
import com.example.budgetwise.product.entity.ProductInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
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
        WHERE p.productInfo.id = :productId AND p.marketLocation.id = :marketId 
        AND p.targetDate = :targetDate 
        ORDER BY p.createdAt DESC LIMIT 1
    """)
    Optional<PricePredictions> findLatestPrediction(
            @Param("productId") Long productId,
            @Param("marketId") Long marketId,
            @Param("targetDate") LocalDate targetDate);
}

