package com.practice.knowheart.service;

import com.practice.knowheart.exception.ConversationLimitExceededException;
import com.practice.knowheart.dto.ConversationSummaryResponse;
import com.practice.knowheart.dto.StoredChatMessageResponse;
import com.practice.knowheart.entity.Conversation;
import com.practice.knowheart.entity.StoredChatMessage;
import com.practice.knowheart.repository.ConversationRepository;
import com.practice.knowheart.repository.StoredChatMessageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final StoredChatMessageRepository messageRepository;
    private final ChatMessagePersistenceService persistenceService;
    private final int maxConversations;

    public ConversationService(ConversationRepository conversationRepository,
                                 StoredChatMessageRepository messageRepository,
                                 ChatMessagePersistenceService persistenceService,
                                 @Value("${knowheart.chat.max-conversations:10}") int maxConversations) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.persistenceService = persistenceService;
        this.maxConversations = maxConversations;
    }

    @Transactional(readOnly = true)
    public List<ConversationSummaryResponse> listConversations(String userId) {
        return conversationRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional
    public ConversationSummaryResponse createConversation(String userId) {
        enforceConversationLimit(userId);

        LocalDateTime now = LocalDateTime.now();
        Conversation conversation = new Conversation();
        conversation.setConversationId(UUID.randomUUID().toString());
        conversation.setUserId(userId);
        conversation.setTitle("新对话");
        conversation.setCreatedAt(now);
        conversation.setUpdatedAt(now);
        conversationRepository.save(conversation);
        return toSummary(conversation);
    }

    @Transactional(readOnly = true)
    public List<StoredChatMessageResponse> getMessages(String userId, String conversationId) {
        Conversation conversation = requireOwnedConversation(userId, conversationId);
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getConversationId()).stream()
                .map(this::toMessageResponse)
                .toList();
    }

    @Transactional
    public void deleteConversation(String userId, String conversationId) {
        Conversation conversation = requireOwnedConversation(userId, conversationId);
        messageRepository.deleteByConversationId(conversation.getConversationId());
        conversationRepository.delete(conversation);
    }

    @Transactional(readOnly = true)
    public String resolveConversationId(String userId, String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return createConversation(userId).getConversationId();
        }
        requireOwnedConversation(userId, conversationId);
        return conversationId;
    }

    /**
     * 在 AI 回复完成后持久化本轮问答（流式/同步统一入口）。
     */
    public void saveExchange(String userId, String conversationId, String userMessage, String assistantMessage) {
        requireOwnedConversation(userId, conversationId);
        persistenceService.persistUserMessage(conversationId, userMessage);
        persistenceService.persistAssistantMessage(conversationId, assistantMessage);
    }

    private Conversation requireOwnedConversation(String userId, String conversationId) {
        return conversationRepository.findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("对话不存在或无权访问"));
    }

    private void enforceConversationLimit(String userId) {
        if (conversationRepository.countByUserId(userId) >= maxConversations) {
            throw new ConversationLimitExceededException(maxConversations);
        }
    }

    private ConversationSummaryResponse toSummary(Conversation conversation) {
        long count = messageRepository.countByConversationId(conversation.getConversationId());
        return new ConversationSummaryResponse(
                conversation.getConversationId(),
                conversation.getTitle(),
                conversation.getUpdatedAt(),
                count);
    }

    private StoredChatMessageResponse toMessageResponse(StoredChatMessage message) {
        return new StoredChatMessageResponse(
                message.getMessageId(),
                message.getRole(),
                message.getContent(),
                message.getCreatedAt());
    }
}
