package com.example.budgetwise.prediction.repository;

import com.example.budgetwise.market.entity.MarketLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface MarketLocationPredictionRepository extends JpaRepository<MarketLocation, Long> {

    @Query("""
    SELECT COUNT(m) FROM MarketLocation m 
    WHERE m.status = 'ACTIVE'
""")
    Integer countActiveMarkets();
}
