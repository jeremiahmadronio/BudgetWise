package com.example.budgetwise.prediction.entity;

import com.example.budgetwise.market.entity.MarketLocation;
import com.example.budgetwise.product.entity.ProductInfo;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Table(name = "price_predictions" ,
                                     indexes = {
                                        @Index(name = "idx_prediction_product_date" ,columnList = "product_info_id, targetDate"),
                                             @Index(name = "idx_prediction_lookup",
                                                     columnList = "product_info_id, market_location_id, targetDate DESC"),
                                             @Index(name = "idx_prediction_status",
                                                     columnList = "status, targetDate")
                                     })
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PricePredictions {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_info_id", nullable = false)
    @JsonBackReference
    private ProductInfo productInfo;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonBackReference
    @JoinColumn(name = "market_location_id", nullable = false)
    private MarketLocation marketLocation;
    
    @Column(nullable = false)
    private Double predictedPrice;
    @Column(nullable = false)
    private Double confidenceScore;
    
    public enum Status{
        NORMAL,ANOMALY,OVERRIDDEN
    }
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;
    
    @Column(nullable = true)
    private Double overridePrice;
    @Column(nullable = true)
    private String overrideReason;
    
    @Column(nullable = false)
    private LocalDate targetDate;
    
    @Column(updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    
}
