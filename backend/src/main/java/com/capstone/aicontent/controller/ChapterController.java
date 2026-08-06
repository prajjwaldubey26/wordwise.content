package com.capstone.aicontent.controller;

import com.capstone.aicontent.dto.ChapterSummaryResponse;
import com.capstone.aicontent.service.ChapterService;
import com.capstone.aicontent.service.CurrentUserService;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController @RequestMapping("/api/chapters")
public class ChapterController {
    private final ChapterService chapters; private final CurrentUserService current;
    public ChapterController(ChapterService chapters, CurrentUserService current) { this.chapters = chapters; this.current = current; }
    @PostMapping(value = "/summarize", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ChapterSummaryResponse summarize(Authentication auth, @RequestParam("file") MultipartFile file, @RequestParam(defaultValue = "medium") String length) { return chapters.summarize(current.get(auth), file, length); }
    @GetMapping("/history") public List<ChapterSummaryResponse> history(Authentication auth) { return chapters.history(current.get(auth)); }
}
