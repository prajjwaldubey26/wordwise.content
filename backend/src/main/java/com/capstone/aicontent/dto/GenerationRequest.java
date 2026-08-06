package com.capstone.aicontent.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record GenerationRequest(
        @NotBlank String prompt,
        @Pattern(regexp = "(?i)neutral|formal|casual|persuasive") String tone,
        @Pattern(regexp = "(?i)article|essay|blog|email|story") String contentType,
        @Min(50) @Max(2000) Integer targetWordCount) { }
