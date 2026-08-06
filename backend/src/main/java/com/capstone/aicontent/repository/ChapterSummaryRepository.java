package com.capstone.aicontent.repository;

import com.capstone.aicontent.entity.ChapterSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChapterSummaryRepository extends JpaRepository<ChapterSummary, Long> {
    List<ChapterSummary> findByUserIdOrderByCreatedAtDesc(Long userId);
    long countByUserId(Long userId);
}
