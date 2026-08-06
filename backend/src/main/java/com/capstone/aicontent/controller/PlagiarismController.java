package com.capstone.aicontent.controller;

import com.capstone.aicontent.dto.*;
import com.capstone.aicontent.service.CurrentUserService;
import com.capstone.aicontent.service.PlagiarismService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/plagiarism")
public class PlagiarismController {
    private final PlagiarismService plagiarism; private final CurrentUserService current;
    public PlagiarismController(PlagiarismService plagiarism, CurrentUserService current) { this.plagiarism = plagiarism; this.current = current; }
    @PostMapping("/check") public PlagiarismResponse check(Authentication auth, @Valid @RequestBody PlagiarismRequest request) { return plagiarism.check(current.get(auth), request.text()); }
    @GetMapping("/history") public List<PlagiarismResponse> history(Authentication auth) { return plagiarism.history(current.get(auth)); }
}
