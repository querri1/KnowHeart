package com.practice.knowheart.repository;

import com.practice.knowheart.entity.StoredChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoredChatMessageRepository extends JpaRepository<StoredChatMessage, String> {

    List<StoredChatMessage> findByConversationIdOrderByCreatedAtAsc(String conversationId);

    List<StoredChatMessage> findByConversationIdOrderByCreatedAtDesc(String conversationId, Pageable pageable);

    void deleteByConversationId(String conversationId);

    long countByConversationId(String conversationId);
}
