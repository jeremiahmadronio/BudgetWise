package com.example.budgetwise.scrapper.service;


import com.example.budgetwise.scrapper.messaging.ScraperProducer;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;

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


    public void initiateManualUpload(MultipartFile file) throws IOException {
        // 1. Convert Bytes to Base64 String
        byte[] fileBytes = file.getBytes();
        String base64Content = Base64.getEncoder().encodeToString(fileBytes);

        // 2. Send to Producer
        scrapperProducer.sendManualUploadRequest(file.getOriginalFilename(), base64Content);
    }


}