package com.capstone.aicontent.dto;

import com.capstone.aicontent.entity.ChapterSummary;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;

public record ChapterSummaryResponse(Long id, String filename, String summary, List<QuizQuestion> questions, Instant createdAt) {
    public static ChapterSummaryResponse from(ChapterSummary item, ObjectMapper mapper) {
        try {
            return new ChapterSummaryResponse(item.getId(), item.getOriginalFilename(), item.getSummary(),
                    mapper.readValue(item.getMcqsJson(), new TypeReference<>() {}), item.getCreatedAt());
        } catch (Exception e) {
            return new ChapterSummaryResponse(item.getId(), item.getOriginalFilename(), item.getSummary(), List.of(), item.getCreatedAt());
        }
    }
}
