package com.example.budgetwise.budgetplan.dto;

import java.util.List;

public record ProductDietaryTagTableResponse (

        long productId,
        String productName,
        String category,
        String localName,
        List<TagOption> tags
        ) {

    public record TagOption (
            long tagId,
            String tagName
    ){

    }
}
