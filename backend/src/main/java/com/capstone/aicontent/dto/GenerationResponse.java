package com.capstone.aicontent.dto;

import com.capstone.aicontent.entity.ContentGeneration;
import java.time.Instant;
public record GenerationResponse(Long id, String prompt, String content, String tone, String contentType, int wordCount, Instant createdAt) {
    public static GenerationResponse from(ContentGeneration g) { return new GenerationResponse(g.getId(), g.getPrompt(), g.getGeneratedContent(), g.getTone(), g.getContentType(), g.getWordCount(), g.getCreatedAt()); }
}
