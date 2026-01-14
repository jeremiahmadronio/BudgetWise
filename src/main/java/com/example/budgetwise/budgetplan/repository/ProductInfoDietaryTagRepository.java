package com.example.budgetwise.budgetplan.repository;

import com.example.budgetwise.product.entity.ProductInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductInfoDietaryTagRepository extends JpaRepository<ProductInfo, Long> {

    long countByStatus(ProductInfo.Status status);

    @Query("SELECT COUNT(DISTINCT p) FROM ProductInfo p JOIN p.productDietaryTags t WHERE p.status = :status")
    long countTaggedProducts(@Param("status") ProductInfo.Status status);

    @Query("SELECT COUNT(p) FROM ProductInfo p WHERE p.productDietaryTags IS EMPTY AND p.status = :status")
    long countUntaggedProducts(@Param("status") ProductInfo.Status status);

    Page<ProductInfo> findAllByStatus(ProductInfo.Status status, Pageable pageable);

    List<ProductInfo> findAllByStatus(ProductInfo.Status status);
}
