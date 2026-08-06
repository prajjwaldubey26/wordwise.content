package com.capstone.aicontent.controller;

import com.capstone.aicontent.dto.*;
import com.capstone.aicontent.service.ContentService;
import com.capstone.aicontent.service.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/content")
public class ContentController {
    private final ContentService content; private final CurrentUserService current;
    public ContentController(ContentService content, CurrentUserService current) { this.content = content; this.current = current; }
    @PostMapping("/generate") public GenerationResponse generate(Authentication auth, @Valid @RequestBody GenerationRequest request) { return content.generate(current.get(auth), request); }
    @GetMapping("/history") public List<GenerationResponse> history(Authentication auth) { return content.history(current.get(auth)); }
}
