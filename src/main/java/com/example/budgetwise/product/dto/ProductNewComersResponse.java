package com.example.budgetwise.product.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductNewComersResponse {

    private long id;

    private String productName;
    private String category;
    private String origin;
    private String localName;
    private String unit;
    private Double price;
    private int totalMarkets;
    private LocalDate detectedDate;


}