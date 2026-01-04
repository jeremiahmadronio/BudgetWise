package com.example.budgetwise.prediction.dto;


import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Dashboard statistics DTO
 * For the stats cards at the top of the dashboard
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DashboardStatsDTO(
        Integer totalProducts,
        Integer activeMarkets,
        Double modelAccuracy,
        Integer anomalies,
        Integer totalPredictions,
        String lastUpdated
) {

    /**
     * Get formatted accuracy string
     */
    public String getFormattedAccuracy() {
        if (modelAccuracy == null) return "N/A";
        return String.format("%.1f%%", modelAccuracy);
    }

    /**
     * Get accuracy status
     */
    public String getAccuracyStatus() {
        if (modelAccuracy == null) return "UNKNOWN";
        if (modelAccuracy >= 85.0) return "EXCELLENT";
        if (modelAccuracy >= 70.0) return "GOOD";
        if (modelAccuracy >= 50.0) return "FAIR";
        return "POOR";
    }

    /**
     * Check if there are critical anomalies
     */
    public boolean hasCriticalAnomalies() {
        return anomalies != null && anomalies > 5;
    }
}