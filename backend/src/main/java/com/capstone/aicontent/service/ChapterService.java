package com.capstone.aicontent.service;

import com.capstone.aicontent.dto.ChapterSummaryResponse;
import com.capstone.aicontent.dto.QuizQuestion;
import com.capstone.aicontent.entity.ChapterSummary;
import com.capstone.aicontent.entity.User;
import com.capstone.aicontent.exception.BadRequestException;
import com.capstone.aicontent.repository.ChapterSummaryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

@Service
public class ChapterService {
    private final ChapterSummaryRepository summaries; private final AiGenerationService ai; private final ObjectMapper mapper;
    public ChapterService(ChapterSummaryRepository summaries, AiGenerationService ai, ObjectMapper mapper) { this.summaries = summaries; this.ai = ai; this.mapper = mapper; }
    public ChapterSummaryResponse summarize(User user, MultipartFile file, String length) {
        validatePdf(file); String extracted = extract(file); int target = switch (length == null ? "medium" : length.toLowerCase()) { case "short" -> 100; case "long" -> 500; default -> 250; };
        String summary = ai.summarize(extracted, target); List<QuizQuestion> quiz = ai.quiz(extracted);
        try {
            ChapterSummary item = new ChapterSummary(); item.setUser(user); item.setOriginalFilename(file.getOriginalFilename() == null ? "chapter.pdf" : file.getOriginalFilename());
            item.setExtractedText(extracted.length() > 50000 ? extracted.substring(0, 50000) : extracted); item.setSummary(summary); item.setMcqsJson(mapper.writeValueAsString(quiz));
            return ChapterSummaryResponse.from(summaries.save(item), mapper);
        } catch (Exception e) { throw new BadRequestException("Could not save the generated chapter summary."); }
    }
    public List<ChapterSummaryResponse> history(User user) { return summaries.findByUserIdOrderByCreatedAtDesc(user.getId()).stream().map(item -> ChapterSummaryResponse.from(item, mapper)).toList(); }
    private void validatePdf(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BadRequestException("Please choose a PDF file.");
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!filename.endsWith(".pdf") || (file.getContentType() != null && !file.getContentType().equalsIgnoreCase("application/pdf"))) throw new BadRequestException("Only PDF files are supported.");
    }
    private String extract(MultipartFile file) {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            String text = new PDFTextStripper().getText(document).replaceAll("\\s+", " ").trim();
            if (text.length() < 40) throw new BadRequestException("No extractable text was found. This may be a scanned-image PDF.");
            return text;
        } catch (BadRequestException e) { throw e; }
        catch (IOException e) { throw new BadRequestException("We could not read that PDF. Please upload a valid text-based PDF."); }
    }
}
