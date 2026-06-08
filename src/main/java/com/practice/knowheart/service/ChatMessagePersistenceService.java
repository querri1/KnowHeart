package com.practice.knowheart.service;

import com.practice.knowheart.entity.Conversation;
import com.practice.knowheart.entity.StoredChatMessage;
import com.practice.knowheart.repository.ConversationRepository;
import com.practice.knowheart.repository.StoredChatMessageRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ChatMessagePersistenceService {

    private final ConversationRepository conversationRepository;
    private final StoredChatMessageRepository messageRepository;

    public ChatMessagePersistenceService(ConversationRepository conversationRepository,
                                         StoredChatMessageRepository messageRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistUserMessage(String conversationId, String text) {
        persistMessage(conversationId, new UserMessage(text));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistAssistantMessage(String conversationId, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        persistMessage(conversationId, new AssistantMessage(text));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistMessage(String conversationId, Message message) {
        MessageType type = message.getMessageType();
        if (type == MessageType.SYSTEM || type == MessageType.TOOL) {
            return;
        }

        StoredChatMessage entity = new StoredChatMessage();
        entity.setMessageId(UUID.randomUUID().toString());
        entity.setConversationId(conversationId);
        entity.setRole(type.name());
        entity.setContent(message.getText());
        entity.setCreatedAt(LocalDateTime.now());
        messageRepository.save(entity);

        if (type == MessageType.USER) {
            maybeUpdateTitle(conversationId, message.getText());
        }
        touchConversation(conversationId);
    }

    private void maybeUpdateTitle(String conversationId, String userText) {
        conversationRepository.findById(conversationId).ifPresent(conversation -> {
            if (conversation.getTitle() != null && !conversation.getTitle().isBlank()
                    && !"新对话".equals(conversation.getTitle())) {
                return;
            }
            conversation.setTitle(buildTitle(userText));
            conversationRepository.save(conversation);
        });
    }

    private void touchConversation(String conversationId) {
        conversationRepository.findById(conversationId).ifPresent(conversation -> {
            conversation.setUpdatedAt(LocalDateTime.now());
            conversationRepository.save(conversation);
        });
    }

    static String buildTitle(String text) {
        if (text == null || text.isBlank()) {
            return "新对话";
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 28) {
            return normalized;
        }
        return normalized.substring(0, 28) + "…";
    }
}
