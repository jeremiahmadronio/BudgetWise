package com.example.budgetwise.market.entity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import java.util.List;
import com.example.budgetwise.product.entity.DailyPriceRecord;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor

@Table(name = "market_location",
        indexes =
        @Index(name = "idx_market_location_name", columnList = "market_location")
)
@Entity
public class MarketLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String marketLocation;

    public enum Type { WET_MARKET, SUPERMARKET};


    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Type type;


    public enum Status { ACTIVE, INACTIVE };

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Status status;

    @Column
    private double latitude;
    @Column
    private double longitude;

    @Column
    private LocalDateTime openingTime;
    @Column
    private LocalDateTime closingTime;

    @Column
    private double ratings;

    @Column(columnDefinition = "TEXT")
    private String description;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
    @Column
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "marketLocation", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<DailyPriceRecord> dailyPriceRecords;


}