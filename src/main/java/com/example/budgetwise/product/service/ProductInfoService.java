package com.example.budgetwise.product.service;


import com.example.budgetwise.exception.ResourcesNotFoundException;
import com.example.budgetwise.market.dto.MarketDetail;
import com.example.budgetwise.market.dto.ProductMarketDetailResponse;
import com.example.budgetwise.market.entity.MarketLocation;
import com.example.budgetwise.product.dto.*;
import com.example.budgetwise.product.entity.DailyPriceRecord;
import com.example.budgetwise.product.entity.ProductInfo;
import com.example.budgetwise.product.repository.DailyPriceRecordRepository;
import com.example.budgetwise.product.repository.ProductDietaryTagRepository;
import com.example.budgetwise.product.repository.ProductInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductInfoService {

    private final ProductInfoRepository productInfoRepository;
    private final ProductDietaryTagRepository productDietaryTagRepository;
    private final DailyPriceRecordRepository dailyPriceRecordRepository;


    /**

     * This method implements a Batch Fetching Strategy to solve the N+1 Query Problem.
     * Instead of querying the database inside a loop, it fetches related data (Markets, Tags)
     * in bulk and merges them in memory.
     * Retrieves a paginated list of products with aggregated details.
     * @param pageable Pagination information (page number, size, sorting).
     * @return A Page of {@link ProductTableResponse} containing product info, latest price, market counts, and tags.
     */
    @Transactional(readOnly = true)
    public Page<ProductTableResponse> displayProducts(Pageable pageable) {

        //  Main Query: Fetch base product details (Name, Price, Status)
        Page<ProductTableResponse> productsPage = productInfoRepository.displayProductTable(pageable);

        if(productsPage.isEmpty()) {
            return Page.empty(pageable);
        }
        //  Extraction: Collect all Product IDs from the current page
        List<Long> productIds = productsPage.stream()
                .map(ProductTableResponse::getId)
                .toList();
        //  Batch Fetching: Retrieve related data for ALL IDs in single queries (High Performance)
        List<ProductDietaryTagRepository.TagProjection> allTags = productDietaryTagRepository.findByProductIdIn(productIds);
        List<DailyPriceRecordRepository.MarketCountProjection> marketCounts = dailyPriceRecordRepository.countMarketsByProductIds(productIds);

        //  In-Memory Mapping: Group Tags by ProductID for fast lookup
        Map<Long, List<String>> allTagsMap = allTags.stream()
                .collect(Collectors.groupingBy(
                        ProductDietaryTagRepository.TagProjection::getProductId,
                        Collectors.mapping(ProductDietaryTagRepository.TagProjection::getDietaryTag, Collectors.toList()

                        )));
        //  In-Memory Mapping: Map Market Counts by ProductID for fast lookup
        Map<Long, Integer> countsMap = marketCounts.stream()
                .collect(Collectors.toMap(
                        DailyPriceRecordRepository.MarketCountProjection::getProductId,
                        p -> p.getTotalMarkets() != null ? p.getTotalMarkets().intValue() : 0                ));


        // Assembly: Iterate through the page and inject the missing data
        productsPage.getContent().forEach(dto -> {
            dto.setDietaryTags(allTagsMap.getOrDefault(dto.getId(), new ArrayList<>()));
            dto.setTotalMarkets(countsMap.getOrDefault(dto.getId(), 0));
        });

        return productsPage;

    }



    /**
     * Aggregates high-level statistics for the products Header.
     * * Business Logic Note:
     * - Currently maps 'PENDING' status to 'Archived' count.
     * - Counts only products that have at least one assigned dietary tag.
     *
     * @return ProductStatsResponse containing real-time counts.
     */
    @Transactional(readOnly = true)
    public ProductStatsResponse getProductStats() {
        long totalProducts = productInfoRepository.count();
        long activeProducts = productInfoRepository.countByStatus(ProductInfo.Status.ACTIVE);
        long totalArchived = productInfoRepository.countByStatus(ProductInfo.Status.PENDING);
        long productsWithTags = productInfoRepository.countProductWithDietaryTag();

        return new ProductStatsResponse(
                totalProducts,
                activeProducts,
                totalArchived,
                productsWithTags
        );
    }



    /**
     * Retrieves newly ingested products marked as PENDING that are not yet active.
     *
     * This method ensures no product name in the result set exists in the ACTIVE status list.
     * Data for each product is mapped using the earliest DailyPriceRecord found.* * @return List of unique products ready for admin review.
     */
    @Transactional(readOnly = true)
    public List<ProductNewComersResponse> findNewComersProducts() {
        // Use optimized query with JOIN FETCH
        List<ProductInfo> pendingProducts = productInfoRepository
                .findByStatusWithPriceRecords(ProductInfo.Status.PENDING);

        List<String> activeProductNames = productInfoRepository
                .findProductNameByStatus(ProductInfo.Status.ACTIVE);

        Set<String> activeNamesSet = new HashSet<>(activeProductNames);

        return pendingProducts.stream()
                .filter(product -> !activeNamesSet.contains(product.getProductName()))
                .map(this::mapToReviewDTO)
                .toList();
    }

    /**
     * Maps ProductInfo to ProductNewComersResponse using its earliest price record.
     * * Logic Highlights:
     * Price, Origin, and Unit are taken from the earliest DailyPriceRecord.
     * Detected Date is sourced from the PriceReport's dateReported.
     * Total Markets counts unique MarketLocation entities linked to the product's price records.
     *
     * * @param product The ProductInfo entity fetched with priceRecords.
     * @return The clean DTO for display.
     */
    @Transactional(readOnly = true)
    private ProductNewComersResponse mapToReviewDTO(ProductInfo product) {
        Double price = 0.0;
        int totalMarkets = 0;
        String origin = product.getLocalName();
        String unit = "N/A";
        LocalDate detectedDate = null;

        List<DailyPriceRecord> priceRecords = product.getPriceRecords();

        if (priceRecords != null && !priceRecords.isEmpty()) {
            DailyPriceRecord firstRecord = priceRecords.stream()
                    .min(Comparator.comparing(DailyPriceRecord::getCreatedAt))
                    .orElse(null);

            Set<String> uniqueMarkets = priceRecords.stream()
                    .map(DailyPriceRecord::getMarketLocation)
                    .filter(ml -> ml != null)
                    .map(MarketLocation::getMarketLocation)
                    .filter(loc -> loc != null && !loc.isBlank())
                    .collect(Collectors.toSet());

            totalMarkets = uniqueMarkets.size();

            if (firstRecord != null) {
                price = firstRecord.getPrice();
                origin = firstRecord.getOrigin();
                unit = firstRecord.getUnit();

                if (firstRecord.getPriceReport() != null) {
                    detectedDate = firstRecord.getPriceReport().getDateReported();
                }
            }
        }

        return new ProductNewComersResponse(
                product.getId(),
                product.getProductName(),
                product.getCategory(),
                origin,
                product.getLocalName(),
                unit,
                price,
                totalMarkets,
                detectedDate
        );
    }



    /**
     * Executes the update operation for a Newcomer product (Edit by ID).
     * It uses the load-update-save pattern.
     *
     * @param ids The ID of the product to be updated.
     * @param request The DTO containing the fields to be modified.
     * @return The updated product data mapped back to the DTO.
     * @throws ResourcesNotFoundException if the product ID does not exist.
     */
    @Transactional
    public UpdateNewComersRequest ManageNewComersProduct(Long ids, UpdateNewComersRequest request) {
        ProductInfo product = productInfoRepository.findById(ids)
                .orElseThrow(() -> new ResourcesNotFoundException("Product", "id", ids));

        if (request.getProductName() != null) {
            product.setProductName(request.getProductName());
        }
        if (request.getCategory() != null) {
            product.setCategory(request.getCategory());
        }
        if (request.getLocalName() != null) {
            product.setLocalName(request.getLocalName());
        }
        ProductInfo updatedProduct = productInfoRepository.save(product);

        return mapToManageDTO(updatedProduct);

    }

    @Transactional(readOnly = true)
    private UpdateNewComersRequest mapToManageDTO(ProductInfo product) {
        String origin = product.getPriceRecords().stream()
                .findFirst()
                .map(r -> r.getOrigin())
                .orElse("N/A");

        return new UpdateNewComersRequest(
                product.getId(),
                product.getProductName(),
                product.getCategory(),
                product.getLocalName(),
                origin
        );

    }

    /**
     * Updates the status of a product based on the provided request.
     *
     * @param request The DTO containing the product ID and the new status.
     * @return The updated product status wrapped in a DTO.
     * @throws ResourcesNotFoundException if the product ID does not exist.
     */
    @Transactional
    public UpdateProductStatus updateProductStatus(UpdateProductStatus request) {

        ProductInfo product = productInfoRepository.findById(request.id())
                .orElseThrow(() -> new ResourcesNotFoundException("Product", "id", request.id()));


        product.setStatus(request.newStatus().equalsIgnoreCase(request.newStatus())
                ? ProductInfo.Status.valueOf(request.newStatus())
                : product.getStatus());
        product.setUpdatedAt(java.time.LocalDateTime.now());



        ProductInfo updatedProduct = productInfoRepository.save(product);

        return new UpdateProductStatus(
                updatedProduct.getId(),
                updatedProduct.getStatus().name(),
                "Product status updated successfully."

        );
    }





    @Transactional(readOnly = true)
    public ArchiveStatsResponse getArchiveStats() {
        long inactiveCount = productInfoRepository.countByStatus(ProductInfo.Status.INACTIVE);
        long pendingCount = productInfoRepository.countByStatus(ProductInfo.Status.PENDING);
        long totalArchived = inactiveCount + pendingCount;

        YearMonth currentMonth = YearMonth.now();
        LocalDateTime startOfMonth = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = currentMonth.atEndOfMonth().atTime(LocalTime.MAX);


        long archivedThisMonth = productInfoRepository.countByStatusInAndUpdatedAtBetween(
                Arrays.asList(ProductInfo.Status.INACTIVE, ProductInfo.Status.PENDING),
                startOfMonth,
                endOfMonth
        );


        return new ArchiveStatsResponse(totalArchived, archivedThisMonth, pendingCount);
    }



    @Transactional(readOnly = true)
    public Page<ArchiveTableResponse> getArchivedProducts(String searchQuery, Pageable pageable) {

        // Target: INACTIVE at PENDING
        List<ProductInfo.Status> archivedStatuses = Arrays.asList(
                ProductInfo.Status.INACTIVE,
                ProductInfo.Status.PENDING
        );

        if (searchQuery != null && !searchQuery.isBlank()) {
            return productInfoRepository.findArchivedProductsWithSearch(
                    archivedStatuses, searchQuery, pageable);
        } else {
            return productInfoRepository.findArchivedProductsNoSearch(
                    archivedStatuses, pageable);
        }
    }

    /**
     * Retrieves comprehensive product details along with all markets where the product is available.
     *
     * This method fetches:
     * - Basic product information (ID and name)
     * - List of market locations selling this product (with operating hours and type)
     *
     * @param productId the unique identifier of the product
     * @return ProductMarketDetailResponse containing product info and associated market details
     * @throws ResourcesNotFoundException if no product exists with the given ID
     *
     * @example
     * // Returns product "Rice" with markets: "Market A", "Market B", etc.
     * getProductMarketDetails(123L);
     */
    @Transactional(readOnly = true)
    public ProductMarketDetailResponse getProductMarketDetails(Long productId) {
        ProductInfo product = productInfoRepository.findById(productId)
                .orElseThrow(() -> new ResourcesNotFoundException("Product", "id", productId));


        List<MarketDetail> marketDetails = productInfoRepository.findMarketDetailsByProductId(productId);

        return new ProductMarketDetailResponse(
                product.getId(),
                product.getProductName(),
                marketDetails
        );
    }
}