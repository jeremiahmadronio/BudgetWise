package com.example.budgetwise.scrapper.messaging;
import com.example.budgetwise.scrapper.dto.ScrapeRequestDto;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class ScraperProducer {

    private final RabbitTemplate rabbitTemplate;

    public ScraperProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendScrapeRequest(String url) {

        ScrapeRequestDto requestDTO = new ScrapeRequestDto(url,null,null);



        System.out.println("Sending JSON request for URL: " + url);

        rabbitTemplate.convertAndSend("scrape_request_queue", requestDTO);
    }


    public void sendManualUploadRequest(String filename, String base64Content) {
        ScrapeRequestDto requestDTO = new ScrapeRequestDto(null, base64Content, filename);

        System.out.println("Sending Manual PDF Request: " + filename);
        rabbitTemplate.convertAndSend("scrape_request_queue", requestDTO);
    }



}