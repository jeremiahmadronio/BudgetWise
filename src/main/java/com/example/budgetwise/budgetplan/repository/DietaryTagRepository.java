package com.example.budgetwise.budgetplan.repository;

import com.example.budgetwise.budgetplan.entity.DietaryTag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DietaryTagRepository extends JpaRepository<DietaryTag, Long> {

    boolean existsByTagNameIgnoreCase(String tagName);

    List<DietaryTag>findAllByOrderByTagNameAsc();

    List<DietaryTag> findAllByStatusOrderByTagNameAsc(DietaryTag.Status status);
    long countByStatus(DietaryTag.Status status);

    long countByStatusAndUpdatedAtBetween(DietaryTag.Status status, LocalDateTime start, LocalDateTime end);


    @Query("SELECT COUNT(d) FROM DietaryTag d WHERE d.status = 'ACTIVE' AND SIZE(d.productDietaryTags) = 0")
    long countUnusedActiveTags();

    @Query("SELECT d FROM DietaryTag d WHERE d.status = :status AND " +
            "(LOWER(d.tagName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(d.tagDescription) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<DietaryTag> findArchivedTagsWithSearch(DietaryTag.Status status, String query, Pageable pageable);

    Page<DietaryTag> findByStatus(DietaryTag.Status status, Pageable pageable);

    @Modifying
    @Query("UPDATE DietaryTag d SET d.status = :status, d.updatedAt = CURRENT_TIMESTAMP WHERE d.id IN :ids")
    int updateStatusForIds(DietaryTag.Status status, List<Long> ids);
}