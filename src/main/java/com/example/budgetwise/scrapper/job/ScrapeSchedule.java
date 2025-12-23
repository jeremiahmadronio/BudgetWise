package com.example.budgetwise.scrapper.job;
import com.example.budgetwise.scrapper.service.ScrapperTrigger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScrapeSchedule {

    private final ScrapperTrigger scrapeTrigger;

    public ScrapeSchedule(ScrapperTrigger scrapeTrigger) {
        this.scrapeTrigger = scrapeTrigger;
    }

    @Scheduled(cron = "0 0 1 * * ?") // Runs daily at 1 AM
    public void runDailyScrape() {
        scrapeTrigger.initiateTrigger();
    }

}
