package com.capstone.aicontent.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "chapter_summaries")
public class ChapterSummary {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id") private User user;
    @Column(nullable = false) private String originalFilename;
    @Column(nullable = false, columnDefinition = "LONGTEXT") private String extractedText;
    @Column(nullable = false, columnDefinition = "LONGTEXT") private String summary;
    @Column(nullable = false, columnDefinition = "LONGTEXT") private String mcqsJson;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @PrePersist void onCreate() { createdAt = Instant.now(); }
    public Long getId() { return id; }
    public User getUser() { return user; } public void setUser(User user) { this.user = user; }
    public String getOriginalFilename() { return originalFilename; } public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }
    public String getExtractedText() { return extractedText; } public void setExtractedText(String extractedText) { this.extractedText = extractedText; }
    public String getSummary() { return summary; } public void setSummary(String summary) { this.summary = summary; }
    public String getMcqsJson() { return mcqsJson; } public void setMcqsJson(String mcqsJson) { this.mcqsJson = mcqsJson; }
    public Instant getCreatedAt() { return createdAt; }
}
