package com.example.budgetwise.product.entity;



import com.example.budgetwise.market.entity.MarketLocation;
import com.example.budgetwise.price_report.entity.PriceReport;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "daily_price_record",
                       indexes = {
                                @Index(name = "idx_dpr_market_location", columnList = "market_location_id"),
                                @Index(name = "idx_dpr_product_info", columnList = "product_info_id"),
                                @Index(name = "idx_dpr_price_report", columnList = "price_report_id"),
                               @Index(name = "idx_dpr_market_product_latest", columnList = "product_info_id, market_location_id, id DESC")
                                        })

public class DailyPriceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column
    private double price;
    @Column(length = 20)
    private String unit;
    @Column(length = 250)
    private String origin;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_info_id" , nullable = false)
    private ProductInfo productInfo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "price_report_id" , nullable = false)
    private PriceReport priceReport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "market_location_id")
    private MarketLocation marketLocation;
}