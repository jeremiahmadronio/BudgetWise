package com.example.budgetwise.analytics.service;

import com.example.budgetwise.analytics.dto.*;
import com.example.budgetwise.analytics.repository.projection.SummaryStatsProjection; // Refactored import
import com.example.budgetwise.analytics.repository.AnalyticsRepository;
import com.example.budgetwise.market.entity.MarketLocation;
import com.example.budgetwise.market.repository.MarketLocationRepository;
import com.example.budgetwise.product.repository.PriceReportRepository;
import com.example.budgetwise.product.repository.ProductInfoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.time.LocalDate;

@Service
public class AnalyticsService {

    private final AnalyticsRepository recordRepository;
    private final MarketLocationRepository marketRepository;
    private final ProductInfoRepository productInfoRepository;
    private final PriceReportRepository reportRepository;

    public AnalyticsService(AnalyticsRepository recordRepository, MarketLocationRepository marketRepository, ProductInfoRepository productInfoRepository, PriceReportRepository reportRepository) {
        this.recordRepository = recordRepository;
        this.marketRepository = marketRepository;
        this.productInfoRepository = productInfoRepository;
        this.reportRepository = reportRepository;
    }


    @Transactional(readOnly = true)
    public ProductAnalyticsResponse getProductAnalytics(String productName, Long marketId, int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);

        Double lastKnownPrice = recordRepository.findLatestPriceBefore(productName, marketId, startDate)
                .orElse(0.0);

        List<PriceHistoryPoint> rawHistory;
        String marketLabel;

        if (marketId != null && marketId > 0) {
            marketLabel = marketRepository.findById(marketId)
                    .map(MarketLocation::getMarketLocation).orElse("Unknown Market");
            rawHistory = recordRepository.findHistoryByMarket(productName, marketId, startDate);
        } else {
            marketLabel = "National Average";
            rawHistory = recordRepository.findHistoryNationalAverage(productName, startDate);
        }

        SummaryStatsProjection stats = recordRepository.findCombinedStats(productName, marketId, startDate).orElse(null);

        Map<LocalDate, Double> priceMap = rawHistory.stream()
                .collect(Collectors.toMap(PriceHistoryPoint::date, PriceHistoryPoint::price));

        List<PriceHistoryPoint> filledHistory = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            Double priceToday = priceMap.get(date);
            if (priceToday != null) {
                lastKnownPrice = priceToday;
            }
            double cleanPrice = Math.round(lastKnownPrice * 100.0) / 100.0;
            filledHistory.add(new PriceHistoryPoint(date, cleanPrice));
        }

        double min = 0.0, max = 0.0, avg = 0.0;
        if (stats != null && stats.getMinPrice() != null) {
            min = stats.getMinPrice();
            max = stats.getMaxPrice();
            avg = Math.round(stats.getAvgPrice() * 100.0) / 100.0;
        }

        return new ProductAnalyticsResponse(
                productName, marketLabel, min, max, avg,
                calculateVolatility(min, max, avg),
                filledHistory
        );
    }

    private String calculateVolatility(Double min, Double max, Double avg) {
        if (avg == null || avg == 0) return "Low";
        double fluctuation = ((max - min) / avg) * 100;
        if (fluctuation < 5) return "Low";
        if (fluctuation < 15) return "Medium";
        return "High";
    }

    /**
     * Aggregates all available market and product lookups for frontend discovery.
     * * <p>Performance Optimization:</p>
     * <ul>
     * <li>Uses interface-based projections to fetch only necessary ID and Name fields.</li>
     * <li>Prevents "God Repository" anti-pattern by delegating queries to their respective feature modules.</li>
     * <li>Returns a combined {@link DiscoveryResponse} to minimize initial load API calls from the UI.</li>
     * </ul>
     * * @return A {@link DiscoveryResponse} containing lists of {@link MarketLookup} and {@link ProductLookup}.
     */
    @Transactional(readOnly = true)
    public DiscoveryResponse getDiscoveryData() {
        List<MarketLookup> markets = marketRepository.findAllMarketLookups();
        List<ProductLookup> products = productInfoRepository.findAllProductLookups();

        return new DiscoveryResponse(markets, products);
    }


    @Transactional(readOnly = true)
    public List<MarketComparisonChart> getMarketComparison(String productName, Long marketId, int days) {
        LocalDate startDate = LocalDate.now().minusDays(days - 1);

        List<MarketComparisonChart> rawData = recordRepository.findMarketComparisonData(
                productName, marketId, startDate
        );

        if (rawData.isEmpty()) {
            return Collections.emptyList();
        }

        return rawData.stream()
                .map(item -> new MarketComparisonChart(
                        item.marketName(),
                        Math.round(item.averagePrice() * 100.0) / 100.0,
                        item.isTargetMarket()
                ))
                .collect(Collectors.toList());
    }


    @Transactional(readOnly = true)
    public GainerDeclinerResponse getMarketTopMovements(Long marketId, int days) {
        LocalDate endDate = reportRepository.findLatestReportDate().orElse(LocalDate.now());
        LocalDate startDate = endDate.minusDays(days);

        // 1. Fetch Data
        List<PriceMovement> currentData = recordRepository.findMarketPricesOnDate(marketId, endDate);
        List<PriceMovement> pastData = recordRepository.findMarketPricesOnDate(marketId, startDate);

        Map<String, Double> pastPriceMap = pastData.stream()
                .collect(Collectors.toMap(PriceMovement::productName, PriceMovement::currentPrice));

        List<PriceMovement> allMovements = currentData.stream()
                .filter(curr -> pastPriceMap.containsKey(curr.productName()))
                .map(curr -> {
                    double oldPrice = pastPriceMap.get(curr.productName());
                    double newPrice = curr.currentPrice();

                    if (oldPrice <= 0) return null;

                    double change = Math.round(((newPrice - oldPrice) / oldPrice * 100) * 100.0) / 100.0;



                    return new PriceMovement(
                            curr.productName(),
                            Math.round(newPrice * 100.0) / 100.0,
                            Math.round(oldPrice * 100.0) / 100.0,
                            change,
                            (change > 0) ? "UP" : (change < 0) ? "DOWN" : "STABLE"
                    );
                })
                .filter(Objects::nonNull) // Remove nulls from 'oldPrice <= 0' check
                .collect(Collectors.toList());

        int totalGainers = (int) allMovements.stream()
                .filter(m -> m.percentageChange() > 0)
                .count();

        int totalDecliners = (int) allMovements.stream()
                .filter(m -> m.percentageChange() < 0)
                .count();

        List<PriceMovement> topGainers = allMovements.stream()
                .filter(m -> m.percentageChange() > 0)
                .sorted(Comparator.comparing(PriceMovement::percentageChange).reversed()) // Highest % first
                .limit(5)
                .toList();

        List<PriceMovement> topDecliners = allMovements.stream()
                .filter(m -> m.percentageChange() < 0)
                .sorted(Comparator.comparing(PriceMovement::percentageChange)) // Most negative first
                .limit(5)
                .toList();

        return new GainerDeclinerResponse(
                topGainers,
                topDecliners,
                totalGainers,
                totalDecliners
        );
    }


}