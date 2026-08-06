package com.capstone.aicontent.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "plagiarism_checks")
public class PlagiarismCheck {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id") private User user;
    @Column(nullable = false, columnDefinition = "LONGTEXT") private String documentText;
    @Column(nullable = false) private Double similarityScore;
    @Column(nullable = false, columnDefinition = "LONGTEXT") private String matchedSources;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @PrePersist void onCreate() { createdAt = Instant.now(); }
    public Long getId() { return id; }
    public User getUser() { return user; } public void setUser(User user) { this.user = user; }
    public String getDocumentText() { return documentText; } public void setDocumentText(String documentText) { this.documentText = documentText; }
    public Double getSimilarityScore() { return similarityScore; } public void setSimilarityScore(Double similarityScore) { this.similarityScore = similarityScore; }
    public String getMatchedSources() { return matchedSources; } public void setMatchedSources(String matchedSources) { this.matchedSources = matchedSources; }
    public Instant getCreatedAt() { return createdAt; }
}
