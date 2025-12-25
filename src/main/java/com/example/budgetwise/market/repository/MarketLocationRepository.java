package com.example.budgetwise.market.repository;



import com.example.budgetwise.market.dto.MarketDetail;
import com.example.budgetwise.market.dto.MarketProductsResponse;
import com.example.budgetwise.market.dto.MarketTableResponse;
import com.example.budgetwise.market.dto.MarketViewResponse;
import com.example.budgetwise.market.entity.MarketLocation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MarketLocationRepository extends JpaRepository<MarketLocation, Long> {


    List<MarketLocation> findByMarketLocationIn(List<String> marketLocations);

    @Query("""
            SELECT COUNT(m) FROM MarketLocation m
            WHERE m.type = :marketType
            """)
    long countByMarketType(@Param("marketType")  MarketLocation.Type marketType);

    @Query("""
            SELECT COUNT(m) FROM MarketLocation m
            WHERE m.status = :status
            """)
    long countByStatus(MarketLocation.Status status);



    /**
     * Fetches market details along with a count of distinct products available in each market.
     * Utilizes a constructor expression (JPQL SELECT new) to return a DTO directly,
     * avoiding the overhead of fetching full Entity objects and N+1 query issues.
     *
     * @param pageable Pagination info.
     * @return Page of MarketTableResponse.
     */
    @Query("""
        SELECT new com.example.budgetwise.market.dto.MarketTableResponse(
            m.id,
            m.marketLocation,
            m.type,
            m.status,
            COUNT(DISTINCT dpr.productInfo.id)
        )
        FROM MarketLocation m
        LEFT JOIN m.dailyPriceRecords dpr
        GROUP BY m.id, m.marketLocation, m.type, m.status
        ORDER BY m.marketLocation ASC
        """)
    Page<MarketTableResponse> displayMarketInformation(Pageable pageable);



    /**
     * Projects market and product data into a DTO using a JPQL constructor expression.
     * Performance Note:This query uses a correlated subquery:
     * @code (SELECT COUNT(dprSub) ...) to calculate the total products for the market.
     * While this avoids a separate N+1 query for the count, it runs per row.
     * Optimized for single-market retrieval via ID.
     *
     * @param marketId The ID of the market to filter by.
     * @return A list of flattened DTOs containing market info, product details, and price.
     */
    @Query("""
    SELECT new com.example.budgetwise.market.dto.MarketProductsResponse(
        m.id,
        m.marketLocation,
        m.type,
        (SELECT COUNT(dprSub) FROM DailyPriceRecord dprSub WHERE dprSub.marketLocation.id = m.id),
        p.productName,
        p.category,
        dpr.price,
        dpr.priceReport.dateReported
    )
    FROM MarketLocation m
    JOIN m.dailyPriceRecords dpr
    JOIN dpr.productInfo p
    WHERE m.id = :marketId       
    ORDER BY m.marketLocation ASC, p.productName ASC
    """)
    List<MarketProductsResponse> displayProductByMarketId(@Param("marketId") Long marketId);


    Optional<MarketLocation> findById(Long id);

    boolean existsByMarketLocation(String marketLocation);
    boolean existsByMarketLocationAndIdNot(String marketLocation, Long id);


    /**
     * Finds all unique market locations that sell a specific product.
     *
     * Query Navigation:
     * 1. Starts from ProductInfo (the product)
     * 2. Joins to PriceRecords (prices for that product)
     * 3. Joins to MarketLocation (markets with those prices)
     * 4. Returns distinct market details
     * This prevents duplicate markets if a product has multiple price records
     * at the same location (e.g., price history updates).
     *Returns empty list if product has no price records or markets
     */
    @Query("SELECT DISTINCT NEW com.example.budgetwise.market.dto.MarketDetail(" +
            "ml.id, ml.marketLocation , ml.type, ml.openingTime, ml.closingTime) " +
            "FROM ProductInfo p " +
            "JOIN p.priceRecords pr " +
            "JOIN pr.marketLocation ml " +
            "WHERE p.id = :productId")
    List<MarketDetail> findMarketDetailsByProductId(@Param("productId") Long productId);



    @Query("""
        SELECT new com.example.budgetwise.market.dto.MarketViewResponse(
            m.id, 
            m.marketLocation, 
            m.type, 
            m.status, 
            m.latitude, 
            m.longitude,
            (SELECT COUNT(DISTINCT dpr.productInfo.id) 
             FROM DailyPriceRecord dpr 
             WHERE dpr.marketLocation = m),
            m.openingTime, 
            m.closingTime, 
            m.ratings, 
            m.description, 
            m.createdAt,        
            m.updatedAt
        )
        FROM MarketLocation m
        WHERE m.id = :id
    """)
    Optional<MarketViewResponse> findMarketViewById(@Param("id") Long id);



    @Modifying(clearAutomatically = true)
    @Query("UPDATE MarketLocation m SET m.status = :status, m.updatedAt = CURRENT_TIMESTAMP WHERE m.id IN :ids")
    int updateMarketStatusBulk(@Param("status") MarketLocation.Status status, @Param("ids") List<Long> ids);
}