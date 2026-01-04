package com.example.budgetwise.prediction.scheduler;

import com.example.budgetwise.prediction.service.PricePredictionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Background service for automated price predictions
 * Runs scheduled tasks automatically based on cron expressions
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledPredictionService {

    private final PricePredictionService predictionService;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Daily prediction generation
     * Runs every day at midnight (12:00 AM)
     *
     * Uses last 30 days to predict next 7 days
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void dailyPredictionGeneration() {
        String timestamp = LocalDateTime.now().format(FORMATTER);

        log.info("╔════════════════════════════════════════════════════════════╗");
        log.info("║  AUTOMATED TASK: Daily Prediction Generation          ║");
        log.info("║  Started at: {}                             ║", timestamp);
        log.info("╚════════════════════════════════════════════════════════════╝");

        try {
            long startTime = System.currentTimeMillis();

            // Run bulk prediction
            predictionService.runBulkPrediction();

            long duration = (System.currentTimeMillis() - startTime) / 1000;
            log.info("Daily prediction completed in {} seconds", duration);

        } catch (Exception e) {
            log.error(" Failed to complete daily prediction generation", e);
        }
    }

    /**
     * Alternative schedules (uncomment to use):
     */

    // Run twice daily (6 AM and 6 PM)
    // @Scheduled(cron = "0 0 6,18 * * *")
    // public void twiceDaily() {
    //     log.info("Running 6 AM/6 PM prediction update...");
    //     predictionService.runBulkPrediction();
    // }

    // Run every 6 hours
    // @Scheduled(cron = "0 0 */6 * * *")
    // public void every6Hours() {
    //     log.info("Running 6-hourly prediction update...");
    //     predictionService.runBulkPrediction();
    // }

    // Run every weekday at 9 AM (Monday-Friday)
    // @Scheduled(cron = "0 0 9 * * MON-FRI")
    // public void weekdayMorning() {
    //     log.info("Running weekday morning prediction update...");
    //     predictionService.runBulkPrediction();
    // }

    /**
     * Health check - logs every hour to confirm scheduler is running
     * Runs at minute 0 of every hour
     */
    @Scheduled(cron = "0 0 * * * *")
    public void hourlyHealthCheck() {
        log.debug("Scheduler health check - System active at {}",
                LocalDateTime.now().format(FORMATTER));
    }
}