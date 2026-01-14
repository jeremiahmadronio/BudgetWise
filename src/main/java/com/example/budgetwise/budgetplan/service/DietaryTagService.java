package com.example.budgetwise.budgetplan.service;

import com.example.budgetwise.budgetplan.dto.CreateTagRequest;
import com.example.budgetwise.budgetplan.dto.DietaryStatsResponse;
import com.example.budgetwise.budgetplan.dto.DietaryTagOptionResponse;
import com.example.budgetwise.budgetplan.dto.ProductDietaryTagTableResponse;
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
import java.util.List;

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
        return dietaryTagRepository.findAllByOrderByTagNameAsc().stream()
                .map(tag -> new DietaryTagOptionResponse(
                        tag.getId(),
                        tag.getTagName(),
                        tag.getTagDescription(),
                        tag.getUpdatedAt()
                )).toList();
    }

}
