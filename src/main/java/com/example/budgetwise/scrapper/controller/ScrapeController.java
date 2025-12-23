package com.example.budgetwise.scrapper.controller;


import com.example.budgetwise.scrapper.service.ScrapperTrigger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/scrape")
public class ScrapeController {

    private final ScrapperTrigger scrapperTrigger;

    public ScrapeController(ScrapperTrigger scrapperTrigger) {
        this.scrapperTrigger = scrapperTrigger;
    }

    @PostMapping("/trigger")
    public ResponseEntity<String> TriggerScrape() {
        scrapperTrigger.initiateTrigger();
        return ResponseEntity.accepted().body("Scraping request has been dispatched to Python worker.");
    }

}
