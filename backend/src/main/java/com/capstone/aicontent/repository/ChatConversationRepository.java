package com.capstone.aicontent.repository;

import com.capstone.aicontent.entity.ChatConversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {
    List<ChatConversation> findByUserIdOrderByUpdatedAtDesc(Long userId);
    Optional<ChatConversation> findByIdAndUserId(Long id, Long userId);
}
