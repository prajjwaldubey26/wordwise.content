package com.capstone.aicontent.repository;

import com.capstone.aicontent.entity.ContentGeneration;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ContentGenerationRepository extends JpaRepository<ContentGeneration, Long> {
    List<ContentGeneration> findByUserIdOrderByCreatedAtDesc(Long userId);
    long countByUserId(Long userId);
}
