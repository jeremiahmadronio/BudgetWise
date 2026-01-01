package com.example.budgetwise.market.repository;



import com.example.budgetwise.analytics.dto.MarketLookup;
import com.example.budgetwise.market.dto.MarketDetail;
import com.example.budgetwise.market.dto.MarketProductsResponse;
import com.example.budgetwise.market.dto.MarketTableResponse;
import com.example.budgetwise.market.dto.MarketViewResponse;
import com.example.budgetwise.market.entity.MarketLocation;
import com.example.budgetwise.market.repository.projection.MarketProductRow;
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

    @Query("SELECT m.id AS id, m.marketLocation AS marketName, m.type AS type FROM MarketLocation m ORDER BY m.marketLocation ASC")
    List<MarketLookup> findAllMarketLookups();

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



    @Query("""
    SELECT 
        m.id AS marketId, 
        m.marketLocation AS marketName, 
        m.type AS marketType,
        p.productName AS productName, 
        p.category AS productCategory, 
        dpr.price AS productPrice, 
        pr.dateReported AS dateRecorded
    FROM DailyPriceRecord dpr
    JOIN dpr.marketLocation m
    JOIN dpr.productInfo p
    JOIN dpr.priceReport pr
    WHERE m.id = :marketId
    AND dpr.id = (
        SELECT MAX(d2.id) 
        FROM DailyPriceRecord d2 
        WHERE d2.marketLocation.id = m.id 
        AND d2.productInfo.id = p.id
    )
    ORDER BY p.productName ASC
""")
    List<MarketProductRow> fetchLatestMarketProducts(@Param("marketId") Long marketId);



    Optional<MarketLocation> findById(Long id);

    boolean existsByMarketLocation(String marketLocation);
    boolean existsByMarketLocationAndIdNot(String marketLocation, Long id);


  



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