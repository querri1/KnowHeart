package com.practice.knowheart.controller;

import com.practice.knowheart.dto.ConversationSummaryResponse;
import com.practice.knowheart.dto.StoredChatMessageResponse;
import com.practice.knowheart.entity.AppUser;
import com.practice.knowheart.service.AuthService;
import com.practice.knowheart.service.ConversationService;
import com.practice.knowheart.util.AuthTokenUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/knowheart/conversations")
public class ConversationController {

    private final AuthService authService;
    private final ConversationService conversationService;

    public ConversationController(AuthService authService, ConversationService conversationService) {
        this.authService = authService;
        this.conversationService = conversationService;
    }

    @GetMapping
    public ResponseEntity<List<ConversationSummaryResponse>> list(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        AppUser user = authService.requireValidUser(AuthTokenUtils.extractToken(authorization));
        return ResponseEntity.ok(conversationService.listConversations(user.getUserId()));
    }

    @PostMapping
    public ResponseEntity<ConversationSummaryResponse> create(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        AppUser user = authService.requireValidUser(AuthTokenUtils.extractToken(authorization));
        return ResponseEntity.ok(conversationService.createConversation(user.getUserId()));
    }

    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<List<StoredChatMessageResponse>> messages(
            @PathVariable String conversationId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        AppUser user = authService.requireValidUser(AuthTokenUtils.extractToken(authorization));
        return ResponseEntity.ok(conversationService.getMessages(user.getUserId(), conversationId));
    }

    @DeleteMapping("/{conversationId}")
    public ResponseEntity<Void> delete(
            @PathVariable String conversationId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        AppUser user = authService.requireValidUser(AuthTokenUtils.extractToken(authorization));
        conversationService.deleteConversation(user.getUserId(), conversationId);
        return ResponseEntity.ok().build();
    }
}
