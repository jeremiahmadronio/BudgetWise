package com.example.budgetwise.budgetplan.repository;

import com.example.budgetwise.budgetplan.entity.DietaryTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DietaryTagRepository extends JpaRepository<DietaryTag, Long> {

    boolean existsByTagNameIgnoreCase(String tagName);
}