package com.capstone.aicontent.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "content_generations")
public class ContentGeneration {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id") private User user;
    @Column(nullable = false, columnDefinition = "TEXT") private String prompt;
    @Column(nullable = false, columnDefinition = "LONGTEXT") private String generatedContent;
    @Column(nullable = false, length = 30) private String tone;
    @Column(nullable = false, length = 30) private String contentType;
    @Column(nullable = false) private Integer wordCount;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @PrePersist void onCreate() { createdAt = Instant.now(); }
    public Long getId() { return id; }
    public User getUser() { return user; } public void setUser(User user) { this.user = user; }
    public String getPrompt() { return prompt; } public void setPrompt(String prompt) { this.prompt = prompt; }
    public String getGeneratedContent() { return generatedContent; } public void setGeneratedContent(String generatedContent) { this.generatedContent = generatedContent; }
    public String getTone() { return tone; } public void setTone(String tone) { this.tone = tone; }
    public String getContentType() { return contentType; } public void setContentType(String contentType) { this.contentType = contentType; }
    public Integer getWordCount() { return wordCount; } public void setWordCount(Integer wordCount) { this.wordCount = wordCount; }
    public Instant getCreatedAt() { return createdAt; }
}
