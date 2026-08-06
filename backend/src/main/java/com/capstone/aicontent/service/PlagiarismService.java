package com.capstone.aicontent.service;

import com.capstone.aicontent.dto.MatchedSource;
import com.capstone.aicontent.dto.PlagiarismResponse;
import com.capstone.aicontent.entity.ChapterSummary;
import com.capstone.aicontent.entity.ContentGeneration;
import com.capstone.aicontent.entity.PlagiarismCheck;
import com.capstone.aicontent.entity.User;
import com.capstone.aicontent.exception.BadRequestException;
import com.capstone.aicontent.repository.ChapterSummaryRepository;
import com.capstone.aicontent.repository.ContentGenerationRepository;
import com.capstone.aicontent.repository.PlagiarismCheckRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Offline plagiarism comparison using 5-word shingles, Jaccard similarity, and cosine similarity. */
@Service
public class PlagiarismService {
    private final PlagiarismCheckRepository checks; private final ContentGenerationRepository generations; private final ChapterSummaryRepository summaries; private final ObjectMapper mapper;
    public PlagiarismService(PlagiarismCheckRepository checks, ContentGenerationRepository generations, ChapterSummaryRepository summaries, ObjectMapper mapper) { this.checks = checks; this.generations = generations; this.summaries = summaries; this.mapper = mapper; }
    public PlagiarismResponse check(User user, String text) {
        List<String> submitted = shingles(text);
        if (submitted.isEmpty()) throw new BadRequestException("Please submit at least five words so we can compare 5-word phrases.");
        List<MatchedSource> matches = new ArrayList<>();
        for (ContentGeneration item : generations.findAll()) addMatch(matches, submitted, item.getGeneratedContent(), item.getId(), "Generated content #" + item.getId());
        for (ChapterSummary item : summaries.findAll()) addMatch(matches, submitted, item.getSummary(), item.getId(), "Chapter summary: " + item.getOriginalFilename());
        for (PlagiarismCheck item : checks.findAll()) addMatch(matches, submitted, item.getDocumentText(), item.getId(), "Previous document check #" + item.getId());
        matches.sort(Comparator.comparingDouble(MatchedSource::score).reversed());
        List<MatchedSource> top = matches.stream().filter(m -> m.score() > 0).limit(5).toList();
        double score = top.isEmpty() ? 0 : top.get(0).score();
        String verdict = score < 25 ? "Original" : score < 60 ? "Minor overlap detected" : "Likely plagiarized";
        try {
            PlagiarismCheck item = new PlagiarismCheck(); item.setUser(user); item.setDocumentText(text.trim()); item.setSimilarityScore(score); item.setMatchedSources(mapper.writeValueAsString(top));
            item = checks.save(item); return new PlagiarismResponse(item.getId(), score, verdict, top, item.getCreatedAt());
        } catch (Exception e) { throw new BadRequestException("Could not save this plagiarism check."); }
    }
    public List<PlagiarismResponse> history(User user) {
        return checks.findByUserIdOrderByCreatedAtDesc(user.getId()).stream().map(item -> new PlagiarismResponse(item.getId(), item.getSimilarityScore(), verdict(item.getSimilarityScore()), parseMatches(item.getMatchedSources()), item.getCreatedAt())).toList();
    }
    private void addMatch(List<MatchedSource> matches, List<String> submitted, String candidate, Long id, String label) {
        List<String> other = shingles(candidate); if (other.isEmpty()) return;
        Set<String> submittedSet = new HashSet<>(submitted), otherSet = new HashSet<>(other);
        Set<String> intersection = new HashSet<>(submittedSet); intersection.retainAll(otherSet);
        Set<String> union = new HashSet<>(submittedSet); union.addAll(otherSet);
        double jaccard = union.isEmpty() ? 0 : (double) intersection.size() / union.size();
        double cosine = cosine(submitted, other);
        matches.add(new MatchedSource(id, label, round((jaccard * .55 + cosine * .45) * 100)));
    }
    private List<String> shingles(String text) {
        String[] words = text.toLowerCase().replaceAll("[^a-z0-9\\s]", " ").trim().split("\\s+");
        List<String> values = new ArrayList<>(); for (int i = 0; i + 5 <= words.length; i++) values.add(String.join(" ", java.util.Arrays.copyOfRange(words, i, i + 5)));
        return values;
    }
    private double cosine(List<String> a, List<String> b) {
        Map<String, Integer> aTf = tf(a), bTf = tf(b); double dot = 0, aMag = 0, bMag = 0;
        for (String key : aTf.keySet()) dot += aTf.get(key) * bTf.getOrDefault(key, 0);
        for (int value : aTf.values()) aMag += value * value; for (int value : bTf.values()) bMag += value * value;
        return aMag == 0 || bMag == 0 ? 0 : dot / (Math.sqrt(aMag) * Math.sqrt(bMag));
    }
    private Map<String, Integer> tf(List<String> shingles) { Map<String, Integer> map = new HashMap<>(); shingles.forEach(s -> map.merge(s, 1, Integer::sum)); return map; }
    private List<MatchedSource> parseMatches(String json) { try { return mapper.readValue(json, new TypeReference<>() {}); } catch (Exception ignored) { return List.of(); } }
    private String verdict(double score) { return score < 25 ? "Original" : score < 60 ? "Minor overlap detected" : "Likely plagiarized"; }
    private double round(double value) { return Math.round(value * 100.0) / 100.0; }
}
