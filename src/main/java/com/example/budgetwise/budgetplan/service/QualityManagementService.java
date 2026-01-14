package com.example.budgetwise.budgetplan.service;

import com.example.budgetwise.budgetplan.dto.QualityIssueResponse;
import com.example.budgetwise.budgetplan.repository.ProductInfoDietaryTagRepository;
import com.example.budgetwise.product.entity.ProductInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class QualityManagementService {


    private final ProductInfoDietaryTagRepository productInfoDietaryTagRepository;

    private static final Map<String, List<String>> CONFLICT_RULES = Map.of(
            "Vegan", List.of(
                    "Pork Meat Products",
                    "Fish Products",
                    "Chicken",
                    "Beef",
                    "Processed Meat"
            ),
            "Vegetarian", List.of(
                    "Pork Meat Products",
                    "Fish Products",
                    "Chicken",
                    "Beef",
                    "Processed Meat"
            ),
            "Pescatarian", List.of(
                    "Pork Meat Products",
                    "Chicken",
                    "Beef"
            ),
            "Halal", List.of(
                    "Pork Meat Products",
                    "Alcohol",
                    "Bacon",
                    "Ham"
            )
    );


    private static final List<String> REQUIRES_HALAL_INFO = List.of(
            "Beef",
            "Chicken",
            "Processed Meat",
            "Canned Goods"
    );

    @Transactional(readOnly = true)
    public List<QualityIssueResponse> scanForQualityIssues() {
        List<QualityIssueResponse> issues = new ArrayList<>();

        // Fetch ALL Active Products
        List<ProductInfo> products = productInfoDietaryTagRepository.findAllByStatus(ProductInfo.Status.ACTIVE );

        for (ProductInfo product : products) {
            String category = product.getCategory();
            String name = product.getProductName();

            if (category == null) continue;

            List<String> tags = product.getProductDietaryTags().stream()
                    .map(link -> link.getDietaryTag().getTagName())
                    .toList();


            if (tags.isEmpty()) {
                issues.add(new QualityIssueResponse(
                        product.getId(), name, category,
                        "Untagged", "MEDIUM",
                        "Product has no dietary tags. It will be invisible in filtered searches.",
                        "Add at least one tag (e.g., Low Fat, Vegan, etc.)",
                        tags, product.getUpdatedAt()
                ));
                continue; // Skip other checks since tags are empty
            }


            for (String tagName : tags) {
                if (CONFLICT_RULES.containsKey(tagName)) {
                    List<String> forbiddenCategories = CONFLICT_RULES.get(tagName);

                    boolean isConflict = forbiddenCategories.stream()
                            .anyMatch(forbidden -> forbidden.equalsIgnoreCase(category));

                    if (isConflict) {
                        issues.add(new QualityIssueResponse(
                                product.getId(), name, category,
                                "Conflicting", "HIGH",
                                "Tag '" + tagName + "' is not allowed for category '" + category + "'.",
                                "Remove \"" + tagName + "\" tag",
                                tags, product.getUpdatedAt()
                        ));
                    }
                }
            }

            if (REQUIRES_HALAL_INFO.stream().anyMatch(c -> c.equalsIgnoreCase(category))) {

                // Check kung meron na siyang Halal tag
                boolean hasHalalInfo = tags.stream()
                        .anyMatch(t -> t.equalsIgnoreCase("Halal"));

                if (!hasHalalInfo) {
                    issues.add(new QualityIssueResponse(
                            product.getId(), name, category,
                            "Incomplete", "HIGH",
                            "Meat/Poultry products must be explicitly tagged as Halal if applicable.",
                            "Add \"Halal\" tag or ignore if Non-Halal",
                            tags, product.getUpdatedAt()
                    ));
                }
            }


            boolean isHighProtein = tags.stream().anyMatch(t -> t.equalsIgnoreCase("High Protein"));
            boolean isLowCalorie = tags.stream().anyMatch(t -> t.equalsIgnoreCase("Low Calorie"));

            if (isHighProtein && isLowCalorie) {
                issues.add(new QualityIssueResponse(
                        product.getId(), name, category,
                        "Suspicious", "MEDIUM",
                        "Unusual nutritional combination (High Protein + Low Calorie). Verify accuracy.",
                        "Confirm nutritional info",
                        tags, product.getUpdatedAt()
                ));
            }
        }

        return issues;
    }
}
