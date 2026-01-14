package com.example.budgetwise.budgetplan.repository;

import com.example.budgetwise.budgetplan.entity.DietaryTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DietaryTagRepository extends JpaRepository<DietaryTag, Long> {

    boolean existsByTagNameIgnoreCase(String tagName);

    List<DietaryTag>findAllByOrderByTagNameAsc();

    List<DietaryTag> findAllByStatusOrderByTagNameAsc(DietaryTag.Status status);
    long countByStatus(DietaryTag.Status status);
}