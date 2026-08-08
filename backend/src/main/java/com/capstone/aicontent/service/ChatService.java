package com.capstone.aicontent.service;

import com.capstone.aicontent.dto.ChatDtos.*;
import com.capstone.aicontent.entity.ChatConversation;
import com.capstone.aicontent.entity.ChatMessage;
import com.capstone.aicontent.entity.User;
import com.capstone.aicontent.exception.BadRequestException;
import com.capstone.aicontent.exception.NotFoundException;
import com.capstone.aicontent.repository.ChatConversationRepository;
import com.capstone.aicontent.repository.ChatMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class ChatService {

    private static final Set<String> ALLOWED_MODELS =
            Set.of("mock", "openai", "nvidia", "anthropic");

    private final ChatConversationRepository conversations;
    private final ChatMessageRepository messages;
    private final AiGenerationService ai;

    public ChatService(
            ChatConversationRepository conversations,
            ChatMessageRepository messages,
            AiGenerationService ai) {
        this.conversations = conversations;
        this.messages = messages;
        this.ai = ai;
    }

    public List<ChatModelOption> models() {
        return ai.availableChatModels().stream()
                .map(m -> new ChatModelOption(
                        String.valueOf(m.get("id")),
                        String.valueOf(m.get("label")),
                        Boolean.TRUE.equals(m.get("available"))
                ))
                .toList();
    }

    public List<ConversationSummaryResponse> list(User user) {
        return conversations.findByUserIdOrderByUpdatedAtDesc(user.getId()).stream()
                .map(ConversationSummaryResponse::from)
                .toList();
    }

    public ConversationDetailResponse get(User user, Long id) {
        ChatConversation conversation = requireOwned(user, id);
        return ConversationDetailResponse.from(
                conversation,
                messages.findByConversationIdOrderByCreatedAtAsc(id)
        );
    }

    public ConversationDetailResponse create(User user, CreateConversationRequest request) {
        String model = normalizeModel(request == null ? null : request.model());
        ChatConversation conversation = new ChatConversation();
        conversation.setUser(user);
        conversation.setTitle("New chat");
        conversation.setModel(model);
        conversation = conversations.save(conversation);
        return ConversationDetailResponse.from(conversation, List.of());
    }

    public ConversationDetailResponse update(
            User user,
            Long id,
            UpdateConversationRequest request) {
        ChatConversation conversation = requireOwned(user, id);
        if (request.title() != null && !request.title().isBlank()) {
            conversation.setTitle(request.title().trim());
        }
        if (request.model() != null && !request.model().isBlank()) {
            conversation.setModel(normalizeModel(request.model()));
        }
        conversation.setUpdatedAt(Instant.now());
        conversation = conversations.save(conversation);
        return ConversationDetailResponse.from(
                conversation,
                messages.findByConversationIdOrderByCreatedAtAsc(id)
        );
    }

    @Transactional
    public void delete(User user, Long id) {
        ChatConversation conversation = requireOwned(user, id);
        messages.deleteByConversationId(conversation.getId());
        conversations.delete(conversation);
    }

    @Transactional
    public ConversationDetailResponse send(
            User user,
            Long id,
            SendMessageRequest request) {
        ChatConversation conversation = requireOwned(user, id);
        String content = request.content().trim();
        if (content.isBlank()) {
            throw new BadRequestException("Message cannot be empty.");
        }

        ChatMessage userMessage = new ChatMessage();
        userMessage.setConversation(conversation);
        userMessage.setRole("user");
        userMessage.setContent(content);
        messages.save(userMessage);

        if ("New chat".equals(conversation.getTitle())) {
            conversation.setTitle(titleFrom(content));
        }

        List<ChatMessage> history =
                messages.findByConversationIdOrderByCreatedAtAsc(conversation.getId());

        List<Map<String, String>> payload = history.stream()
                .map(m -> Map.of("role", m.getRole(), "content", m.getContent()))
                .toList();

        String reply = ai.chat(payload, conversation.getModel());

        ChatMessage assistantMessage = new ChatMessage();
        assistantMessage.setConversation(conversation);
        assistantMessage.setRole("assistant");
        assistantMessage.setContent(reply);
        messages.save(assistantMessage);

        conversation.setUpdatedAt(Instant.now());
        conversations.save(conversation);

        return ConversationDetailResponse.from(
                conversation,
                messages.findByConversationIdOrderByCreatedAtAsc(conversation.getId())
        );
    }

    private ChatConversation requireOwned(User user, Long id) {
        return conversations.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new NotFoundException("Conversation not found"));
    }

    private String normalizeModel(String model) {
        if (model == null || model.isBlank()) {
            return ai.defaultChatModel();
        }
        String normalized = model.toLowerCase(Locale.ROOT);
        if (!ALLOWED_MODELS.contains(normalized)) {
            throw new BadRequestException("Unsupported chat model.");
        }
        return normalized;
    }

    private String titleFrom(String content) {
        String cleaned = content.replaceAll("\\s+", " ").trim();
        if (cleaned.length() <= 48) {
            return cleaned;
        }
        return cleaned.substring(0, 45).trim() + "...";
    }
}
