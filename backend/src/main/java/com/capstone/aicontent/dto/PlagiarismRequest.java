package com.capstone.aicontent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record PlagiarismRequest(@NotBlank @Size(min = 10, max = 50000) String text) { }
