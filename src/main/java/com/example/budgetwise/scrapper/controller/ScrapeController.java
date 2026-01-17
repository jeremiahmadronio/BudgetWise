package com.example.budgetwise.scrapper.controller;


import com.example.budgetwise.scrapper.service.ScrapperTrigger;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/admin/scrape")
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



    @PostMapping(value = "/manual-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadManualReport(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty() || !file.getContentType().equals("application/pdf")) {
            return ResponseEntity.badRequest().body("Invalid file. Please upload a PDF.");
        }

        try {
            scrapperTrigger.initiateManualUpload(file);
            return ResponseEntity.ok("File uploaded and sent to scraper!");
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Upload failed: " + e.getMessage());
        }
    }

}
