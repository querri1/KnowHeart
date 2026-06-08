package com.practice.knowheart.dto;

public class ChatRequest {
    private String message;
    private String userId;
    private String conversationId;

    public ChatRequest() {}

    public ChatRequest(String message, String userId) {
        this.message = message;
        this.userId = userId;
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
}