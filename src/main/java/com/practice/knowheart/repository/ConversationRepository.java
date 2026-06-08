package com.practice.knowheart.repository;

import com.practice.knowheart.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, String> {

    List<Conversation> findByUserIdOrderByUpdatedAtDesc(String userId);

    Optional<Conversation> findByConversationIdAndUserId(String conversationId, String userId);

    long countByUserId(String userId);

    Optional<Conversation> findFirstByUserIdOrderByUpdatedAtAsc(String userId);
}
