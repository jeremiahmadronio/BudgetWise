package com.example.budgetwise.prediction.repository;

import com.example.budgetwise.product.entity.ProductInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductInfoPredictionRepository extends JpaRepository<ProductInfo, Long> {

    //prediction
    @Query("""
    SELECT COUNT(p) FROM ProductInfo p 
    WHERE p.status = 'ACTIVE'
""")
    Integer countActiveProducts();

    Page<ProductInfo> findAllByStatus(ProductInfo.Status status, Pageable pageable);
}
