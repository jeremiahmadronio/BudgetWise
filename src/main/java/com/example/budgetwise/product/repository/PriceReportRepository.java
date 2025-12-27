package com.example.budgetwise.product.repository;

import com.example.budgetwise.product.entity.PriceReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface PriceReportRepository extends JpaRepository<PriceReport, Long> {

    boolean existsByDateReported(LocalDate dateReported);
    @Query("SELECT MAX(pr.dateReported) FROM PriceReport pr")
    Optional<LocalDate> findLatestReportDate();

}