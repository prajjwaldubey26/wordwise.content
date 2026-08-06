package com.capstone.aicontent.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record QuizQuestion(@NotBlank String question, List<String> options, int correctIndex) { }
