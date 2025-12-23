package com.example.budgetwise.scrapper.service;


import com.example.budgetwise.scrapper.messaging.ScraperProducer;
import org.springframework.stereotype.Service;

@Service
public class ScrapperTrigger {

    private final ScraperProducer scrapperProducer;

    public ScrapperTrigger(ScraperProducer scrapperProducer) {
        this.scrapperProducer = scrapperProducer;
    }



    public void initiateTrigger() {

        String targetUrl = "https://www.da.gov.ph/price-monitoring/";

        System.out.println("Initiating scrapper trigger for URL: " + targetUrl);

        scrapperProducer.sendScrapeRequest(targetUrl);

    }


}