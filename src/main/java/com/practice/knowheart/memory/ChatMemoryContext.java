package com.practice.knowheart.memory;

import java.util.Optional;

/**
 * 绑定当前请求应使用的 conversationId，避免默认 Memory Advisor 使用错误的会话键。
 */
public final class ChatMemoryContext {

    private static final ThreadLocal<String> CONVERSATION_ID = new ThreadLocal<>();

    private ChatMemoryContext() {
    }

    public static void set(String conversationId) {
        CONVERSATION_ID.set(conversationId);
    }

    public static Optional<String> get() {
        return Optional.ofNullable(CONVERSATION_ID.get());
    }

    public static void clear() {
        CONVERSATION_ID.remove();
    }
}
