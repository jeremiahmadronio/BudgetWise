package com.example.budgetwise.budgetplan.repository;


import com.example.budgetwise.budgetplan.entity.ProductDietaryTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface ProductDietaryTagRepository extends JpaRepository <ProductDietaryTag, Long>{

    /**
     * Projection interface to avoid loading full Entity state during aggregation.
     */
    interface TagCountProjection {
        Long getProductId();
        Long getTotalTags();
    }

    /**
     * Efficiently counts dietary tags for a batch of products.
     * @param productIds List of IDs from the current page.
     * @return List of product IDs paired with their respective tag counts.
     */
    @Query("""
        SELECT pdt.productInfo.id AS productId, COUNT(pdt.id) AS totalTags
        FROM ProductDietaryTag pdt
        WHERE pdt.productInfo.id IN :productIds
        GROUP BY pdt.productInfo.id
    """)
    List<TagCountProjection> countTagsByProductIds(@Param("productIds") List<Long> productIds);
}