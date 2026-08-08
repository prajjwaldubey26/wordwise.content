package com.capstone.aicontent.service;

import com.capstone.aicontent.exception.BadRequestException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

@Service
public class ChatFileService {

    private static final Set<String> IMAGE_TYPES = Set.of(
            "image/png", "image/jpeg", "image/jpg", "image/webp", "image/gif"
    );

    private static final long MAX_BYTES = 10L * 1024 * 1024;

    private final AiGenerationService ai;

    public ChatFileService(AiGenerationService ai) {
        this.ai = ai;
    }

    public record FileContext(String filename, String kind, String extractedText) {}

    public FileContext read(MultipartFile file, String preferredModel) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Please choose a file to upload.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BadRequestException("File is too large. Maximum size is 10MB.");
        }

        String filename = file.getOriginalFilename() == null ? "upload" : file.getOriginalFilename().trim();
        String lower = filename.toLowerCase(Locale.ROOT);
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);

        try {
            if (lower.endsWith(".pdf") || "application/pdf".equals(contentType)) {
                return new FileContext(filename, "pdf", extractPdf(file));
            }
            if (isImage(lower, contentType)) {
                String description = ai.describeImage(
                        file.getBytes(),
                        contentType.isBlank() ? guessImageType(lower) : contentType,
                        preferredModel
                );
                return new FileContext(filename, "image", description);
            }
        } catch (BadRequestException e) {
            throw e;
        } catch (IOException e) {
            throw new BadRequestException("We could not read that file. Try another PDF or image.");
        }

        throw new BadRequestException("Supported files: PDF, PNG, JPG, JPEG, WEBP, GIF.");
    }

    private boolean isImage(String filename, String contentType) {
        if (IMAGE_TYPES.contains(contentType)) {
            return true;
        }
        return filename.endsWith(".png")
                || filename.endsWith(".jpg")
                || filename.endsWith(".jpeg")
                || filename.endsWith(".webp")
                || filename.endsWith(".gif");
    }

    private String guessImageType(String filename) {
        if (filename.endsWith(".png")) return "image/png";
        if (filename.endsWith(".webp")) return "image/webp";
        if (filename.endsWith(".gif")) return "image/gif";
        return "image/jpeg";
    }

    private String extractPdf(MultipartFile file) {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            String text = new PDFTextStripper().getText(document).replaceAll("\\s+", " ").trim();
            if (text.length() < 20) {
                throw new BadRequestException(
                        "No readable text found in that PDF. It may be a scanned image PDF."
                );
            }
            return clip(text, 14000);
        } catch (BadRequestException e) {
            throw e;
        } catch (IOException e) {
            throw new BadRequestException("We could not read that PDF. Please upload a valid PDF.");
        }
    }

    private String clip(String text, int max) {
        if (text.length() <= max) {
            return text;
        }
        return text.substring(0, max) + "\n\n[Document truncated for length.]";
    }
}
