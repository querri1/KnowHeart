package com.practice.knowheart.memory;

import com.practice.knowheart.entity.StoredChatMessage;
import com.practice.knowheart.repository.ConversationRepository;
import com.practice.knowheart.repository.StoredChatMessageRepository;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 已登录用户的会话写入 MySQL；访客仍用进程内记忆。
 */
@Component
public class HybridChatMemory implements ChatMemory {

    private final InMemoryChatMemory guestMemory = new InMemoryChatMemory();
    private final ConversationRepository conversationRepository;
    private final StoredChatMessageRepository messageRepository;

    public HybridChatMemory(ConversationRepository conversationRepository,
                            StoredChatMessageRepository messageRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    @Override
    public void add(String conversationId, Message message) {
        String resolvedId = resolveConversationId(conversationId);
        if (isPersistedConversation(resolvedId)) {
            // 登录用户的持久化由 ChatController 在流式/同步完成后显式写入
            return;
        }
        guestMemory.add(resolvedId, message);
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        String resolvedId = resolveConversationId(conversationId);
        if (isPersistedConversation(resolvedId)) {
            return;
        }
        guestMemory.add(resolvedId, messages);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Message> get(String conversationId, int lastN) {
        String resolvedId = resolveConversationId(conversationId);
        if (!isPersistedConversation(resolvedId)) {
            return guestMemory.get(resolvedId, lastN);
        }

        List<StoredChatMessage> recent = messageRepository.findByConversationIdOrderByCreatedAtDesc(
                resolvedId, PageRequest.of(0, lastN));
        List<Message> messages = new ArrayList<>(recent.size());
        for (int i = recent.size() - 1; i >= 0; i--) {
            messages.add(toMessage(recent.get(i)));
        }
        return messages;
    }

    @Override
    @Transactional
    public void clear(String conversationId) {
        String resolvedId = resolveConversationId(conversationId);
        if (isPersistedConversation(resolvedId)) {
            messageRepository.deleteByConversationId(resolvedId);
            return;
        }
        guestMemory.clear(resolvedId);
    }

    private boolean isPersistedConversation(String conversationId) {
        return conversationRepository.existsById(conversationId);
    }

    private String resolveConversationId(String conversationId) {
        return ChatMemoryContext.get().orElse(conversationId);
    }

    static Message toMessage(StoredChatMessage entity) {
        String role = entity.getRole();
        if ("ASSISTANT".equals(role)) {
            return new AssistantMessage(entity.getContent());
        }
        if ("SYSTEM".equals(role)) {
            return new SystemMessage(entity.getContent());
        }
        return new UserMessage(entity.getContent());
    }
}
