package com.example.budgetwise.product.controller;

import com.example.budgetwise.product.dto.ArchiveStatsResponse;
import com.example.budgetwise.product.dto.ArchiveTableResponse;
import com.example.budgetwise.product.dto.BulkUpdateProductStatus;
import com.example.budgetwise.product.dto.UpdateProductStatus;
import com.example.budgetwise.product.service.ArchiveProductsService;
import com.example.budgetwise.product.service.ProductInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("api/v1/archive")
@RequiredArgsConstructor
public class ArchiveProductController {
    
    private final ArchiveProductsService archiveProducts;
    private final ProductInfoService productInfoService;


    @GetMapping("archive/stats")
    public ResponseEntity<ArchiveStatsResponse> getStats() {
        return ResponseEntity.ok(archiveProducts.getArchiveStats());
    }


    @GetMapping("archive/table")
    public ResponseEntity<Page<ArchiveTableResponse>> getArchivedProducts(
            @RequestParam(required = false) String search,

            @PageableDefault(size = 7, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(archiveProducts.getArchivedProducts(search, pageable));
    }


    @PutMapping("/updateStatus")
    public ResponseEntity<UpdateProductStatus> updateProductStatus(@RequestBody UpdateProductStatus request) {

        UpdateProductStatus response = productInfoService.updateProductStatus(request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/bulk-status")
    public ResponseEntity<String> bulkUpdateStatus(@RequestBody BulkUpdateProductStatus request) {
        productInfoService.bulkUpdateStatus(request);
        return ResponseEntity.ok("Successfully updated status for selected products.");
    }
    
}
