package com.capstone.aicontent.dto;

import com.capstone.aicontent.entity.ChatConversation;
import com.capstone.aicontent.entity.ChatMessage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class ChatDtos {
    private ChatDtos() {}

    public record CreateConversationRequest(
            @Pattern(regexp = "mock|openai|nvidia|anthropic", message = "model must be mock, openai, nvidia, or anthropic")
            String model
    ) {}

    public record SendMessageRequest(
            @NotBlank @Size(max = 8000) String content,
            @Pattern(regexp = "mock|openai|nvidia|anthropic", message = "model must be mock, openai, nvidia, or anthropic")
            String model
    ) {}

    public record UpdateConversationRequest(
            @Size(max = 180) String title,
            @Pattern(regexp = "mock|openai|nvidia|anthropic", message = "model must be mock, openai, nvidia, or anthropic")
            String model
    ) {}

    public record ChatMessageResponse(Long id, String role, String content, Instant createdAt) {
        public static ChatMessageResponse from(ChatMessage message) {
            return new ChatMessageResponse(
                    message.getId(),
                    message.getRole(),
                    message.getContent(),
                    message.getCreatedAt()
            );
        }
    }

    public record ConversationSummaryResponse(
            Long id,
            String title,
            String model,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static ConversationSummaryResponse from(ChatConversation conversation) {
            return new ConversationSummaryResponse(
                    conversation.getId(),
                    conversation.getTitle(),
                    conversation.getModel(),
                    conversation.getCreatedAt(),
                    conversation.getUpdatedAt()
            );
        }
    }

    public record ConversationDetailResponse(
            Long id,
            String title,
            String model,
            Instant createdAt,
            Instant updatedAt,
            List<ChatMessageResponse> messages
    ) {
        public static ConversationDetailResponse from(
                ChatConversation conversation,
                List<ChatMessage> messages) {
            return new ConversationDetailResponse(
                    conversation.getId(),
                    conversation.getTitle(),
                    conversation.getModel(),
                    conversation.getCreatedAt(),
                    conversation.getUpdatedAt(),
                    messages.stream().map(ChatMessageResponse::from).toList()
            );
        }
    }

    public record ChatModelOption(String id, String label, boolean available) {}
}
