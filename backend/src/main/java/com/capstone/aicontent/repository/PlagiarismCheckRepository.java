package com.capstone.aicontent.repository;

import com.capstone.aicontent.entity.PlagiarismCheck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface PlagiarismCheckRepository extends JpaRepository<PlagiarismCheck, Long> {
    List<PlagiarismCheck> findByUserIdOrderByCreatedAtDesc(Long userId);
    long countByUserId(Long userId);
    @Query("select avg(p.similarityScore) from PlagiarismCheck p where p.user.id = :userId")
    Double averageScoreByUserId(Long userId);
}
