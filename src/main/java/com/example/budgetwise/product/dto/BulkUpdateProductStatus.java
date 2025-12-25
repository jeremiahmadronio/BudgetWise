package com.example.budgetwise.product.dto;

import com.example.budgetwise.product.entity.ProductInfo;

import java.util.List;

public record BulkUpdateProductStatus
        (
                List<Long> ids,
                String newStatus
        ){


}
