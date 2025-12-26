package com.example.budgetwise.market.service;



import com.example.budgetwise.exception.ResourcesNotFoundException;
import com.example.budgetwise.market.dto.*;
import com.example.budgetwise.market.entity.MarketLocation;
import com.example.budgetwise.market.repository.MarketLocationRepository;
import com.example.budgetwise.market.repository.projection.MarketProductRow;
import com.example.budgetwise.product.entity.ProductInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MarketLocationService {

    private final MarketLocationRepository marketLocationRepository;


    /**
     * Aggregates key statistics for the market dashboard.
     * Executes separate counts for total, active status, and market types.
     *
     * @return MarketStatsResponse containing current counts of markets by category.
     */
    @Transactional(readOnly = true)
    public MarketStatsResponse getMarketStats() {
        long totalMarkets = marketLocationRepository.count();
        long activeMarkets = marketLocationRepository.countByStatus(com.example.budgetwise.market.entity.MarketLocation.Status.ACTIVE);
        long totalWetMarkets = marketLocationRepository.countByMarketType(com.example.budgetwise.market.entity.MarketLocation.Type.WET_MARKET);
        long totalSupermarkets = marketLocationRepository.countByMarketType(com.example.budgetwise.market.entity.MarketLocation.Type.SUPERMARKET);

        return new MarketStatsResponse(totalMarkets, activeMarkets, totalSupermarkets,totalWetMarkets);

    }

    /**
     * Retrieves a paginated list of markets with their associated product counts.
     * Uses a projection DTO to optimize data fetching performance.
     *
     * @param pageable Pagination and sorting information provided by the controller.
     * @return A Page of MarketTableResponse containing market details and product availability count.
     */
    @Transactional(readOnly = true)
    public Page<MarketTableResponse> displayMarketTableInfo(Pageable pageable) {
        return marketLocationRepository.displayMarketInformation(pageable);
    }



    /**
     * Fetches all products associated with a specific market location.
     * * <p>Refactored Logic:</p>
     * <ul>
     * <li>Uses Interface Projection (MarketProductRow) for high-performance data fetching.</li>
     * <li>Eliminated SQL correlated subqueries for counting to prevent O(N) database overhead.</li>
     * <li>Calculates 'totalProducts' on the application side (Java) to reduce DB load.</li>
     * </ul>
     * * @param marketId The unique identifier of the market.
     * @return A list of {@link MarketProductPriceView} containing product details and price history.
     */
    @Transactional(readOnly = true)
    public List<MarketProductPriceView> getMarketProducts(Long marketId) {
        List<MarketProductRow> rawData = marketLocationRepository.fetchRawMarketProducts(marketId);

        if (rawData.isEmpty()) return List.of();

        long total = rawData.size();

        return rawData.stream()
                .map(row -> new MarketProductPriceView(
                        row.getMarketId(),
                        row.getMarketName(),
                        row.getMarketType(),
                        total,
                        row.getProductName(),
                        row.getProductCategory(),
                        row.getProductPrice(),
                        row.getDateRecorded()
                ))
                .toList();
    }

    /**
     * Updates the status for one or more market locations.
     * PERFORMANCE STRATEGY:
     * - Uses a single @Modifying JPQL query to update all IDs in one database round-trip.
     * - Updates the 'updatedAt' timestamp directly in the database.
     * - Avoids memory overhead by not loading MarketLocation entities.
     * * @param request DTO containing a list of market IDs and the target status.
     */
    @Transactional
    public void updateMarketStatuses(UpdateMarketStatus request) {
        if (request.ids() == null || request.ids().isEmpty()) {
            throw new IllegalArgumentException("Market IDs list cannot be empty.");
        }

        int updatedCount = marketLocationRepository.updateMarketStatusBulk(
                request.newStatus(),
                request.ids()
        );

        if (updatedCount == 0) {
            throw new RuntimeException("No markets were updated. Check if the IDs are valid.");
        }
    }

    /**
     * Creates and persists a new Market Location.
     * Performs validation to ensure the market name is unique before saving.
     * Sets default values for Status (ACTIVE), Ratings (0.0), and Audit timestamps.
     *
     * @param request The data transfer object (Record) containing the market details.
     * Must not be null.
     * @return The persisted {@link MarketLocation} entity with generated ID.
     * @throws IllegalArgumentException if a market with the same location name already exists.
     */
    @Transactional
    public MarketLocation addMarket(CreateMarket request) {

        if (marketLocationRepository.existsByMarketLocation(request.marketLocation())) {
            throw new IllegalArgumentException("Market location already exists.");
        }

        MarketLocation market = new MarketLocation();

        market.setMarketLocation(request.marketLocation());
        market.setType(request.type());

        market.setStatus(Optional.ofNullable(request.status())
                .orElse(MarketLocation.Status.ACTIVE));

        market.setLatitude(request.latitude());
        market.setLongitude(request.longitude());
        market.setOpeningTime(request.openingTime());
        market.setClosingTime(request.closingTime());
        market.setDescription(request.description());

        market.setRatings(0.0);
        market.setUpdatedAt(LocalDateTime.now());

        return marketLocationRepository.save(market);
    }


    /**
     * Updates an existing market location based on the provided ID and request data.
     * <p>
     * This method performs the following steps:
     * 1. Verifies the existence of the market.
     * 2. Checks for name uniqueness (excluding the current market being updated).
     * 3. Maps new values to the entity.
     * 4. Updates the 'updatedAt' timestamp.
     * 5. Persists the changes to the database.
     * </p>
     *
     * @param id      The unique identifier of the market to be updated.
     * @param request The DTO containing updated information (name, coordinates, etc.).
     * @throws RuntimeException         if the market with the given ID does not exist.
     * @throws IllegalArgumentException  if the new market name is already taken by another record.
     */
    @Transactional
    public void updateMarket(Long id, UpdateMarket request) {
        MarketLocation market = marketLocationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Market not found with id: " + id));

        if (marketLocationRepository.existsByMarketLocationAndIdNot(request.marketLocation(), id)) {
            throw new IllegalArgumentException("Market name '" + request.marketLocation() + "' already exists.");
        }

        market.setMarketLocation(request.marketLocation());
        market.setType(request.type());
        market.setLatitude(request.latitude());
        market.setLongitude(request.longitude());
        market.setOpeningTime(request.openingTime());
        market.setClosingTime(request.closingTime());
        market.setDescription(request.description());
        market.setRatings(request.ratings());


        market.setUpdatedAt(LocalDateTime.now());

        marketLocationRepository.save(market);
    }




    @Transactional(readOnly = true)
    public MarketViewResponse getMarketById(Long id) {
        return marketLocationRepository.findMarketViewById(id)
                .orElseThrow(() -> new RuntimeException("Market with ID " + id + " not found"));
    }

}