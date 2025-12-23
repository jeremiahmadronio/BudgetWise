package com.example.budgetwise.product.repository;

import com.example.budgetwise.product.entity.DietaryTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DietaryTagRepository extends JpaRepository<DietaryTag, Long> {
}