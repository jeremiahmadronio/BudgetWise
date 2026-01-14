package com.example.budgetwise.budgetplan.service;

import com.example.budgetwise.budgetplan.dto.CreateTagRequest;
import com.example.budgetwise.budgetplan.dto.DietaryStatsResponse;
import com.example.budgetwise.budgetplan.dto.ProductDietaryTagTableResponse;
import com.example.budgetwise.budgetplan.entity.DietaryTag;
import com.example.budgetwise.budgetplan.repository.DietaryTagRepository;
import com.example.budgetwise.budgetplan.repository.ProductInfoDietaryTagRepository;
import com.example.budgetwise.product.entity.ProductInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DietaryTagService {

    private final DietaryTagRepository dietaryTagRepository;
    private final ProductInfoDietaryTagRepository productInfoDietaryTagRepository;


    @Transactional(readOnly = true)
    public DietaryStatsResponse getDietaryStats() {
        ProductInfo.Status activeStatus = ProductInfo.Status.ACTIVE;

        long totalProducts = productInfoDietaryTagRepository.countByStatus(activeStatus);
        long taggedProducts = productInfoDietaryTagRepository.countTaggedProducts(activeStatus);
        long untaggedProducts = productInfoDietaryTagRepository.countUntaggedProducts(activeStatus);
        long totalOptions = dietaryTagRepository.count();

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



}
