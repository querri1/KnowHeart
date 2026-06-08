package com.practice.knowheart.dto;

import java.time.LocalDateTime;

public class ConversationSummaryResponse {

    private String conversationId;
    private String title;
    private LocalDateTime updatedAt;
    private long messageCount;

    public ConversationSummaryResponse() {
    }

    public ConversationSummaryResponse(String conversationId, String title, LocalDateTime updatedAt, long messageCount) {
        this.conversationId = conversationId;
        this.title = title;
        this.updatedAt = updatedAt;
        this.messageCount = messageCount;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public long getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(long messageCount) {
        this.messageCount = messageCount;
    }
}
