package com.capstone.aicontent.dto;

import java.time.Instant;
import java.util.List;
public record PlagiarismResponse(Long id, double score, String verdict, List<MatchedSource> matches, Instant createdAt) { }
