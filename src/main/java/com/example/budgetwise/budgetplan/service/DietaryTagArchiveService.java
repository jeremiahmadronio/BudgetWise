package com.example.budgetwise.budgetplan.service;

import com.example.budgetwise.budgetplan.dto.DietaryArchiveResponse;
import com.example.budgetwise.budgetplan.dto.DietaryArchiveStatsResponse;
import com.example.budgetwise.budgetplan.entity.DietaryTag;
import com.example.budgetwise.budgetplan.repository.DietaryTagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DietaryTagArchiveService {

    private final DietaryTagRepository dietaryTagRepository;


    @Transactional(readOnly = true)
    public DietaryArchiveStatsResponse getDietaryArchiveStats() {
        long totalArchived = dietaryTagRepository.countByStatus(DietaryTag.Status.INACTIVE);

        YearMonth currentMonth = YearMonth.now();
        LocalDateTime startOfMonth = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = currentMonth.atEndOfMonth().atTime(LocalTime.MAX);

        long archivedThisMonth = dietaryTagRepository.countByStatusAndUpdatedAtBetween(
                DietaryTag.Status.INACTIVE,
                startOfMonth,
                endOfMonth
        );


        long unusedTags = dietaryTagRepository.countUnusedActiveTags();

        return new DietaryArchiveStatsResponse(totalArchived, archivedThisMonth, unusedTags);
    }

    @Transactional(readOnly = true)
    public Page<DietaryArchiveResponse> getArchivedTags(String searchQuery, Pageable pageable) {
        DietaryTag.Status archivedStatus = DietaryTag.Status.INACTIVE;
        Page<DietaryTag> tagPage;

        if (searchQuery != null && !searchQuery.isBlank()) {
            tagPage = dietaryTagRepository.findArchivedTagsWithSearch(
                    archivedStatus,
                    searchQuery.trim(),
                    pageable
            );
        } else {
            tagPage = dietaryTagRepository.findByStatus(
                    archivedStatus,
                    pageable
            );
        }

        // Map Entity to DTO
        return tagPage.map(tag -> new DietaryArchiveResponse(
                tag.getId(),
                tag.getTagName(),
                tag.getTagDescription(),
                tag.getUpdatedAt()
        ));
    }

    @Transactional
    public int updateDietaryTagStatus(List<Long> tagIds, DietaryTag.Status newStatus) {
        // Validation
        if (tagIds == null || tagIds.isEmpty()) {
            throw new IllegalArgumentException("Tag IDs list cannot be empty");
        }

        int updatedCount = dietaryTagRepository.updateStatusForIds(newStatus, tagIds);

        if (updatedCount == 0) {
            throw new IllegalArgumentException("No tags were updated. Check if IDs exist.");
        }

        return updatedCount;
    }
}
