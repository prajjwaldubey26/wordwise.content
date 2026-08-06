package com.capstone.aicontent.service;

import com.capstone.aicontent.dto.GenerationRequest;
import com.capstone.aicontent.dto.GenerationResponse;
import com.capstone.aicontent.entity.ContentGeneration;
import com.capstone.aicontent.entity.User;
import com.capstone.aicontent.repository.ContentGenerationRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ContentService {
    private final ContentGenerationRepository generations; private final AiGenerationService ai;
    public ContentService(ContentGenerationRepository generations, AiGenerationService ai) { this.generations = generations; this.ai = ai; }
    public GenerationResponse generate(User user, GenerationRequest request) {
        String content = ai.generate(request); ContentGeneration item = new ContentGeneration();
        item.setUser(user); item.setPrompt(request.prompt().trim()); item.setGeneratedContent(content); item.setTone(request.tone().toLowerCase()); item.setContentType(request.contentType().toLowerCase()); item.setWordCount(countWords(content));
        return GenerationResponse.from(generations.save(item));
    }
    public List<GenerationResponse> history(User user) { return generations.findByUserIdOrderByCreatedAtDesc(user.getId()).stream().map(GenerationResponse::from).toList(); }
    private int countWords(String content) { return content.trim().isBlank() ? 0 : content.trim().split("\\s+").length; }
}
