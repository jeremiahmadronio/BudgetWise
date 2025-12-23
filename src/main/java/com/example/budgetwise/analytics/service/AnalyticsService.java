package com.example.budgetwise.analytics.service;

import com.example.budgetwise.analytics.dto.PriceHistoryPoint;
import com.example.budgetwise.analytics.dto.ProductAnalyticsResponse;
import com.example.budgetwise.analytics.dto.SummaryStatsProjection; // Refactored import
import com.example.budgetwise.analytics.repository.AnalyticsRepository;
import com.example.budgetwise.market.repository.MarketLocationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class AnalyticsService {

    private final AnalyticsRepository recordRepository;
    private final MarketLocationRepository marketRepository;

    public AnalyticsService(AnalyticsRepository recordRepository, MarketLocationRepository marketRepository) {
        this.recordRepository = recordRepository;
        this.marketRepository = marketRepository;
    }

    @Transactional(readOnly = true)
    public ProductAnalyticsResponse getProductAnalytics(String productName, Long marketId, int days) {

        LocalDate startDate = LocalDate.now().minusDays(days);
        List<PriceHistoryPoint> history;
        Double min = 0.0, max = 0.0, avg = 0.0;
        String marketLabel;

        // Refactored to use Projection Interface instead of Object[]
        SummaryStatsProjection stats;

        if (marketId != null && marketId > 0) {
            // Specific Market
            marketLabel = marketRepository.findById(marketId)
                    .map(m -> m.getMarketLocation())
                    .orElse("Unknown Market");

            history = recordRepository.findHistoryByMarket(productName, marketId, startDate);
            stats = recordRepository.findStatsByMarket(productName, marketId, startDate).orElse(null);

        } else {
            // National Average
            marketLabel = "National Average";
            history = recordRepository.findHistoryNationalAverage(productName, startDate);
            stats = recordRepository.findStatsNational(productName, startDate).orElse(null);
        }

        // Logic Check: Map stats safely if projection is present
        if (stats != null && stats.getMinPrice() != null) {
            min = stats.getMinPrice();
            max = stats.getMaxPrice();
            avg = stats.getAvgPrice();
        }

        avg = Math.round(avg * 100.0) / 100.0;

        String volatility = calculateVolatility(min, max, avg);

        return new ProductAnalyticsResponse(
                productName,
                marketLabel,
                min,
                max,
                avg,
                volatility,
                history
        );
    }

    private String calculateVolatility(Double min, Double max, Double avg) {
        if (avg == 0) return "Low";

        double diff = max - min;
        double percentageFluctuation = (diff / avg) * 100;

        if (percentageFluctuation < 5) return "Low";
        if (percentageFluctuation < 15) return "Medium";
        return "High";
    }
}