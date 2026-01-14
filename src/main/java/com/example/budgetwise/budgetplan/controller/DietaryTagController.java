package com.example.budgetwise.budgetplan.controller;

import com.example.budgetwise.budgetplan.dto.CreateTagRequest;
import com.example.budgetwise.budgetplan.dto.DietaryStatsResponse;
import com.example.budgetwise.budgetplan.dto.ProductDietaryTagTableResponse;
import com.example.budgetwise.budgetplan.dto.UpdateProductTagsRequest;
import com.example.budgetwise.budgetplan.entity.DietaryTag;
import com.example.budgetwise.budgetplan.service.DietaryTagService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@Controller
@RequestMapping("/api/v1/dietaryTag")
@RequiredArgsConstructor
public class DietaryTagController {

    private final DietaryTagService dietaryTagService;

    @GetMapping("/stats")
    public ResponseEntity<DietaryStatsResponse> getStats() {
        return ResponseEntity.ok(dietaryTagService.getDietaryStats());
    }

    @PostMapping("/createTag")
    public ResponseEntity<Map<String, Object>> createTag(@RequestBody CreateTagRequest createTagRequest) {
        DietaryTag createTag = dietaryTagService.createDietaryTag(createTagRequest);
        return ResponseEntity.ok(Map.of(
                "message", "Dietary tag created successfully",
                "tagId", createTag.getId(),
                "tagName", createTag.getTagName()
        ));
    }

    @GetMapping("/products")
    public ResponseEntity<Page<ProductDietaryTagTableResponse>> getProductsTables(
            @PageableDefault(size = 10, sort = "productName")  Pageable pageable) {

        return  ResponseEntity.ok(dietaryTagService.getProductProductsWithDietaryTags(pageable));

    }


    @PutMapping("/products/{productId}/tags")
    public ResponseEntity<Map<String, String>> updateProductTags(
            @PathVariable Long productId,
            @RequestBody UpdateProductTagsRequest request
    ) {
        dietaryTagService.updateProductDietaryTag(productId, request.tagIds());

        return ResponseEntity.ok(Map.of(
                "message", "Product tags updated successfully"
        ));
    }

}
