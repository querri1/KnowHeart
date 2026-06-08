package com.practice.knowheart.exception;

public class ConversationLimitExceededException extends RuntimeException {

    private final int maxConversations;

    public ConversationLimitExceededException(int maxConversations) {
        super("你最多可以保存 " + maxConversations + " 条对话。请先删除不再需要的对话，再创建新对话。");
        this.maxConversations = maxConversations;
    }

    public int getMaxConversations() {
        return maxConversations;
    }
}
