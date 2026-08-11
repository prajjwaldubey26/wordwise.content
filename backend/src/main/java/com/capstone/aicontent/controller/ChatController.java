package com.capstone.aicontent.controller;

import com.capstone.aicontent.dto.ChatDtos.*;
import com.capstone.aicontent.service.ChatService;
import com.capstone.aicontent.service.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chat;
    private final CurrentUserService current;

    public ChatController(ChatService chat, CurrentUserService current) {
        this.chat = chat;
        this.current = current;
    }

    @GetMapping("/models")
    public List<ChatModelOption> models() {
        return chat.models();
    }

    @GetMapping("/conversations")
    public List<ConversationSummaryResponse> list(Authentication auth) {
        return chat.list(current.get(auth));
    }

    @PostMapping("/conversations")
    public ConversationDetailResponse create(
            Authentication auth,
            @RequestBody(required = false) CreateConversationRequest request) {
        return chat.create(current.get(auth), request == null ? new CreateConversationRequest(null) : request);
    }

    @GetMapping("/conversations/{id}")
    public ConversationDetailResponse get(Authentication auth, @PathVariable Long id) {
        return chat.get(current.get(auth), id);
    }

    @PatchMapping("/conversations/{id}")
    public ConversationDetailResponse update(
            Authentication auth,
            @PathVariable Long id,
            @Valid @RequestBody UpdateConversationRequest request) {
        return chat.update(current.get(auth), id, request);
    }

    @DeleteMapping("/conversations/{id}")
    public void delete(Authentication auth, @PathVariable Long id) {
        chat.delete(current.get(auth), id);
    }

    @PostMapping("/conversations/{id}/messages")
    public ConversationDetailResponse sendJson(
            Authentication auth,
            @PathVariable Long id,
            @Valid @RequestBody SendMessageRequest request) {
        return chat.send(current.get(auth), id, request);
    }

    @PostMapping("/conversations/{id}/upload")
    public ConversationDetailResponse sendMultipart(
            Authentication auth,
            @PathVariable Long id,
            @RequestParam(value = "content", required = false) String content,
            @RequestParam(value = "model", required = false) String model,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        return chat.send(current.get(auth), id, content, model, file);
    }
}
