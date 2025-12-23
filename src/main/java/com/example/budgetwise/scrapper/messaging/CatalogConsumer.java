package com.example.budgetwise.scrapper.messaging;



import com.example.budgetwise.product.service.ProductIngestionService;
import com.example.budgetwise.scrapper.dto.ScrapeResultDto;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class CatalogConsumer {

    private final ProductIngestionService productInfoService;

    public CatalogConsumer(ProductIngestionService productInfoService) {
        this.productInfoService = productInfoService;
    }


    @RabbitListener(queues = "scraped_data_queue")
    public void receivePythonResult(ScrapeResultDto resultDTO) {

        System.out.println("Received scrape result for URL: " + resultDTO.status());
        productInfoService.processAndSaveScrapeResult(resultDTO);

        System.out.println("Processed scrape result complete");

    }
}