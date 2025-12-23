package com.example.budgetwise.product.dto;


import com.example.budgetwise.product.entity.ProductInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductTableResponse {

    private long id;

    private String productName;
    private String category;
    private String origin;
    private String localName;
    private String unit;
    private ProductInfo.Status status;
    private Double price;
    private int totalMarkets;
    private List<String> dietaryTags;
    private LocalDate lastUpdated;
}