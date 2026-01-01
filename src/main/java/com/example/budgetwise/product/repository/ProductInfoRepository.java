package com.example.budgetwise.product.repository;



import com.example.budgetwise.analytics.dto.ProductLookup;
import com.example.budgetwise.market.dto.MarketDetail;
import com.example.budgetwise.product.dto.ArchiveTableResponse;
import com.example.budgetwise.product.dto.ProductTableResponse;
import com.example.budgetwise.product.entity.ProductInfo;
import com.example.budgetwise.product.repository.Projection.MarketPriceProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
@Repository
public interface ProductInfoRepository extends JpaRepository <ProductInfo, Long> {

    Optional<ProductInfo> findByCategoryAndProductName(String category, String productName);

    Optional<ProductInfo> findById(Long id);

    boolean existsByProductName(String productName);


    @Query("SELECT p.id AS id, p.productName AS productName, p.category AS category FROM ProductInfo p ORDER BY p.productName ASC")
    List<ProductLookup> findAllProductLookups();
    /**
     * Checks if a product exists based on composite unique constraints.
     */
    @Query(
            """
SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END
FROM ProductInfo p
JOIN p.priceRecords dpr
WHERE p.category = :category
AND p.productName = :productName
AND dpr.origin = :origin
""")
    boolean existsByCategoryAndProductNameAndOrigin(@Param("category") String category,
                                                    @Param("productName") String productName,
                                                    @Param("origin") String origin);


    @Query("""
    SELECT new com.example.budgetwise.product.dto.ProductTableResponse(
        p.id, p.productName, p.category, d.origin, p.localName, d.unit, p.status, d.price, 
        0.0, 0, 0, r.dateReported
    )
    FROM ProductInfo p
    LEFT JOIN p.priceRecords d 
    LEFT JOIN d.priceReport r
    WHERE p.status = com.example.budgetwise.product.entity.ProductInfo.Status.ACTIVE
    AND d.id = (SELECT MAX(d2.id) FROM DailyPriceRecord d2 WHERE d2.productInfo = p)
""")
    Page<ProductTableResponse> displayProductTable(Pageable pageable);

    long countByStatus(ProductInfo.Status status);

    long countByStatusInAndUpdatedAtBetween(
            Collection<ProductInfo.Status> statuses,
            LocalDateTime start,
            LocalDateTime end
    );


    @Query("SELECT COUNT(p) FROM ProductInfo p WHERE p.productDietaryTags IS NOT EMPTY")
    long countProductWithDietaryTag();


    @Query("SELECT p.productName FROM ProductInfo p WHERE p.status = :status")
    List<String> findProductNameByStatus(@Param("status")ProductInfo.Status status);


    @Query("""
    SELECT DISTINCT p FROM ProductInfo p
    LEFT JOIN FETCH p.priceRecords pr
    LEFT JOIN FETCH pr.marketLocation
    WHERE p.status = :status
    """)
    List<ProductInfo> findByStatusWithPriceRecords(@Param("status") ProductInfo.Status status);






    @Query("""
        SELECT new com.example.budgetwise.product.dto.ArchiveTableResponse(
            p.id,
            p.productName,
            p.category,
            r.price,
            r.unit,
            r.origin,
            p.updatedAt
        )
        FROM ProductInfo p
        LEFT JOIN DailyPriceRecord r ON r.id = (
            SELECT MAX(r2.id) 
            FROM DailyPriceRecord r2 
            WHERE r2.productInfo.id = p.id
        )
        WHERE p.status IN :statuses
          AND LOWER(p.productName) LIKE LOWER(CONCAT('%', :search, '%'))
    """)
    Page<ArchiveTableResponse> findArchivedProductsWithSearch(
            @Param("statuses") Collection<ProductInfo.Status> statuses,
            @Param("search") String search,
            Pageable pageable
    );

    // FIXED QUERY 2: No Search
    @Query("""
        SELECT new com.example.budgetwise.product.dto.ArchiveTableResponse(
            p.id,
            p.productName,
            p.category,
            r.price,
            r.unit,
            r.origin,
            p.updatedAt
        )
        FROM ProductInfo p
        LEFT JOIN DailyPriceRecord r ON r.id = (
            SELECT MAX(r2.id) 
            FROM DailyPriceRecord r2 
            WHERE r2.productInfo.id = p.id
        )
        WHERE p.status IN :statuses
    """)
    Page<ArchiveTableResponse> findArchivedProductsNoSearch(
            @Param("statuses") Collection<ProductInfo.Status> statuses,
            Pageable pageable
    );


    @Query(value = """
        SELECT 
            ml.id AS marketId, 
            ml.market_location AS marketName, 
            ml.type AS marketType, 
            ml.opening_time AS marketOpeningTime, 
            ml.closing_time AS marketClosingTime, 
            dpr.price AS currentPrice, 
            dpr.unit AS unit
        FROM market_location ml
        JOIN daily_price_record dpr ON ml.id = dpr.market_location_id
        WHERE dpr.product_info_id = :productId
        AND dpr.id IN (
            SELECT MAX(id) 
            FROM daily_price_record 
            WHERE product_info_id = :productId 
            GROUP BY market_location_id
        )
        """, nativeQuery = true)
    List<MarketPriceProjection> findLatestMarketPricesByProductId(@Param("productId") Long productId);


    @Modifying(clearAutomatically = true)
    @Query("UPDATE ProductInfo p SET p.status = :status WHERE p.id IN :ids")
    int updateStatusForIds(@Param("status") ProductInfo.Status status, @Param("ids") List<Long> ids);
}