package com.example.budgetwise.budgetplan.service;

import com.example.budgetwise.budgetplan.dto.*;
import com.example.budgetwise.budgetplan.entity.DietaryTag;
import com.example.budgetwise.budgetplan.entity.ProductDietaryTag;
import com.example.budgetwise.budgetplan.repository.DietaryTagRepository;
import com.example.budgetwise.budgetplan.repository.ProductDietaryTagRepository;
import com.example.budgetwise.budgetplan.repository.ProductInfoDietaryTagRepository;
import com.example.budgetwise.product.entity.ProductInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DietaryTagService {

    private final DietaryTagRepository dietaryTagRepository;
    private final ProductInfoDietaryTagRepository productInfoDietaryTagRepository;
    private final ProductDietaryTagRepository productDietaryTagRepository;


    @Transactional(readOnly = true)
    public DietaryStatsResponse getDietaryStats() {
        ProductInfo.Status activeStatus = ProductInfo.Status.ACTIVE;

        long totalProducts = productInfoDietaryTagRepository.countByStatus(activeStatus);
        long taggedProducts = productInfoDietaryTagRepository.countTaggedProducts(activeStatus);
        long untaggedProducts = productInfoDietaryTagRepository.countUntaggedProducts(activeStatus);
        long totalOptions = dietaryTagRepository.countByStatus(DietaryTag.Status.ACTIVE);


        return new DietaryStatsResponse(
                totalProducts,
                taggedProducts,
                untaggedProducts,
                totalOptions
        );
    }


    @Transactional
    public DietaryTag createDietaryTag(CreateTagRequest  createTagRequest) {

        String cleanTagName = createTagRequest.tagName().trim();

        if(dietaryTagRepository.existsByTagNameIgnoreCase(cleanTagName)) {
            throw new IllegalArgumentException("Tag with name " + cleanTagName + " already exists");
        }

        DietaryTag dietaryTag = new DietaryTag();
        dietaryTag.setStatus(DietaryTag.Status.ACTIVE);
        dietaryTag.setTagName(cleanTagName);
        dietaryTag.setTagDescription(createTagRequest.tagDescription());
        return dietaryTagRepository.save(dietaryTag);

    }



    @Transactional(readOnly = true)
    public Page<ProductDietaryTagTableResponse> getProductProductsWithDietaryTags(Pageable pageable) {
        Page<ProductInfo> productsPage = productInfoDietaryTagRepository.findAllByStatus(
                ProductInfo.Status.ACTIVE,
                pageable);

        return productsPage.map(product -> {
            List<ProductDietaryTagTableResponse.TagOption> tags = product.getProductDietaryTags()
                    .stream()
                    .filter(link -> link.getDietaryTag().getStatus() == DietaryTag.Status.ACTIVE)
                    .map(link -> new ProductDietaryTagTableResponse.TagOption(
                            link.getDietaryTag().getId(),
                            link.getDietaryTag().getTagName()
                    )).toList();

            return new ProductDietaryTagTableResponse(
                    product.getId(),
                    product.getProductName(),
                    product.getCategory(),
                    product.getLocalName(),
                    tags
            );
        });
    }



    @Transactional
    public void updateProductDietaryTag(Long productId, List<Long> tagIds){

        ProductInfo product =  productInfoDietaryTagRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product with id " + productId + " does not exist"));

        productDietaryTagRepository.deleteAllByProductId(productId);

        if(tagIds == null || tagIds.isEmpty()){
            return;
        }

        List<DietaryTag> tagsToLink = dietaryTagRepository.findAllById(tagIds);
        if (tagsToLink.size() != tagIds.size()) {
            throw new IllegalArgumentException("One or more Dietary Tag IDs are invalid.");
        }

        boolean hasInactiveTag = tagsToLink.stream()
                .anyMatch(tag -> tag.getStatus() != DietaryTag.Status.ACTIVE);

        if (hasInactiveTag) {
            throw new IllegalArgumentException("Cannot assign an archived/inactive tag to a product.");
        }

        List<ProductDietaryTag> newLinks = tagsToLink.stream()
                .map(tag -> {
                    ProductDietaryTag link = new ProductDietaryTag();
                    link.setProductInfo(product);
                    link.setDietaryTag(tag);
                    return link;
                })
                .toList();
        productDietaryTagRepository.saveAll(newLinks);
    }



    @Transactional(readOnly = true)
    public List<DietaryTagOptionResponse>getAllDietaryTagOptions() {
        return dietaryTagRepository.findAllByStatusOrderByTagNameAsc(DietaryTag.Status.ACTIVE).stream()
                .map(tag -> new DietaryTagOptionResponse(
                        tag.getId(),
                        tag.getTagName(),
                        tag.getTagDescription(),
                        tag.getUpdatedAt()
                )).toList();
    }


    @Transactional
    public DietaryTag updateDietaryTagDetails(Long id , UpdateDietaryTagRequest updateDietaryTagRequest) {

        DietaryTag existingTag = dietaryTagRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Dietary Tag with id " + id + " does not exist"));

        String newTagName = updateDietaryTagRequest.tagName().trim();

        if(!existingTag.getTagName().equalsIgnoreCase(newTagName)) {
            if(dietaryTagRepository.existsByTagNameIgnoreCase(newTagName)) {
                throw new IllegalArgumentException("Dietary Tag with name " + newTagName + " already exists");
            }
        }


        existingTag.setTagName(newTagName);
        existingTag.setTagDescription(updateDietaryTagRequest.description());

        return dietaryTagRepository.save(existingTag);


    }




    @Transactional(readOnly = true)
    public List<CategoryCoverageResponse> getCategoryCoverageStats() {
        List<ProductInfo> products = productInfoDietaryTagRepository.findAllByStatus(ProductInfo.Status.ACTIVE);

        Map<String, List<ProductInfo>> groupedByCategory = products.stream()
                .filter(p -> p.getCategory() != null) // Safety check
                .collect(Collectors.groupingBy(ProductInfo::getCategory));

        List<CategoryCoverageResponse> report = new ArrayList<>();

        for (Map.Entry<String, List<ProductInfo>> entry : groupedByCategory.entrySet()) {
            String category = entry.getKey();
            List<ProductInfo> categoryProducts = entry.getValue();

            long totalCount = categoryProducts.size();

            long taggedCount = categoryProducts.stream()
                    .filter(p -> !p.getProductDietaryTags().isEmpty())
                    .count();

            double percentage = totalCount == 0 ? 0 : ((double) taggedCount / totalCount) * 100;

            percentage = Math.round(percentage * 10.0) / 10.0;

            String status;
            if (percentage >= 80) {
                status = "Excellent";
            } else if (percentage >= 50) {
                status = "Good";
            } else {
                status = "Needs Work";
            }

            report.add(new CategoryCoverageResponse(
                    category,
                    taggedCount,
                    totalCount,
                    percentage,
                    status
            ));
        }
        report.sort(Comparator.comparingDouble(CategoryCoverageResponse::coveragePercentage));

        return report;
    }



}
