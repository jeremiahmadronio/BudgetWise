package com.example.budgetwise.price_report.repository;

import com.example.budgetwise.price_report.dto.ReportTableResponse;
import com.example.budgetwise.price_report.entity.PriceReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface PriceReportRepository extends JpaRepository<PriceReport, Long> {

    boolean existsByDateReported(LocalDate dateReported);
    @Query("SELECT MAX(pr.dateReported) FROM PriceReport pr")
    Optional<LocalDate> findLatestReportDate();


    @Query("""
        SELECT new com.example.budgetwise.price_report.dto.ReportTableResponse(
            pr.id,
            pr.dateReported,
            COUNT(DISTINCT dpr.productInfo.id),   
            COUNT(DISTINCT dpr.marketLocation.id),
            COALESCE(pr.durationMS, 0),
            pr.url,
            pr.status
        )
        FROM PriceReport pr
        LEFT JOIN pr.records dpr
        WHERE (:status IS NULL OR pr.status = :status)
        AND (:url IS NULL OR pr.url LIKE %:url%)
        AND (:startDate IS NULL OR pr.dateReported >= :startDate)
        AND (:endDate IS NULL OR pr.dateReported <= :endDate)
        GROUP BY pr.id, pr.dateReported, pr.durationMS, pr.dataSource, pr.url, pr.status
    """)
    Page<ReportTableResponse> findReportsWithStats(
            @Param("status") PriceReport.Status status,
            @Param("url") String url,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );

}