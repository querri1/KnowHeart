package com.practice.knowheart.dto;

import java.time.LocalDateTime;

public class StoredChatMessageResponse {

    private String messageId;
    private String role;
    private String content;
    private LocalDateTime createdAt;

    public StoredChatMessageResponse() {
    }

    public StoredChatMessageResponse(String messageId, String role, String content, LocalDateTime createdAt) {
        this.messageId = messageId;
        this.role = role;
        this.content = content;
        this.createdAt = createdAt;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
