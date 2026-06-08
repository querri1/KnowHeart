package com.practice.knowheart.controller;

import com.practice.knowheart.dto.ChatRequest;
import com.practice.knowheart.entity.AppUser;
import com.practice.knowheart.service.AuthService;
import com.practice.knowheart.service.ConversationService;
import com.practice.knowheart.service.LoveConsultantService;
import com.practice.knowheart.service.UserProfileService;
import com.practice.knowheart.util.AuthTokenUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.concurrent.atomic.AtomicReference;

@RestController
@RequestMapping("/api/knowheart")
public class ChatController {

    private final LoveConsultantService loveService;
    private final AuthService authService;
    private final ConversationService conversationService;

    public ChatController(LoveConsultantService loveService,
                          AuthService authService,
                          ConversationService conversationService) {
        this.loveService = loveService;
        this.authService = authService;
        this.conversationService = conversationService;
    }

    @GetMapping("/health")
    public String health() {
        return "💗 KnowHeart 知意 —— 你的AI恋爱顾问已准备好！";
    }

    @GetMapping("/chat")
    public String chat(@RequestParam String msg) {
        return loveService.chat(msg);
    }

    @GetMapping("/chat/memory")
    public String chatWithMemory(@RequestParam String msg,
                                 @RequestParam(required = false) String userId,
                                 @RequestParam(required = false) String conversationId,
                                 @RequestHeader(value = "Authorization", required = false) String authorization) {
        return resolveSyncChat(msg, userId, conversationId, authorization);
    }

    @GetMapping(value = "/chat/stream", produces = "text/event-stream")
    public Flux<String> chatStream(@RequestParam String msg,
                                   @RequestParam(required = false) String userId,
                                   @RequestParam(required = false) String conversationId,
                                   @RequestHeader(value = "Authorization", required = false) String authorization) {
        return resolveStreamChat(msg, userId, conversationId, authorization);
    }

    @PostMapping("/chat")
    public String chatPost(@RequestBody ChatRequest request,
                           @RequestHeader(value = "Authorization", required = false) String authorization) {
        return resolveSyncChat(request.getMessage(), request.getUserId(), request.getConversationId(), authorization);
    }

    @Autowired
    private UserProfileService userProfileService;

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@RequestParam String userId) {
        var profile = userProfileService.getProfile(userId);
        if (profile.isPresent()) {
            return ResponseEntity.ok(profile.get());
        }
        return ResponseEntity.ok("暂无画像数据，请先发送一些消息");
    }

    @GetMapping("/chat/rag")
    public String chatWithRag(@RequestParam String msg,
                              @RequestParam(required = false) String userId,
                              @RequestParam(required = false) String conversationId,
                              @RequestHeader(value = "Authorization", required = false) String authorization) {
        return resolveSyncChat(msg, userId, conversationId, authorization);
    }

    @GetMapping(value = "/chat/stream/rag", produces = "text/event-stream")
    public Flux<String> chatStreamWithRag(@RequestParam String msg,
                                          @RequestParam(required = false) String userId,
                                          @RequestParam(required = false) String conversationId,
                                          @RequestHeader(value = "Authorization", required = false) String authorization) {
        return resolveStreamChat(msg, userId, conversationId, authorization);
    }

    @GetMapping("/chat/tools")
    public String chatWithTools(@RequestParam String msg,
                                @RequestParam(required = false) String userId,
                                @RequestParam(required = false) String conversationId,
                                @RequestHeader(value = "Authorization", required = false) String authorization) {
        return resolveSyncChat(msg, userId, conversationId, authorization);
    }

    @GetMapping(value = "/chat/stream/tools", produces = "text/event-stream")
    public Flux<String> chatStreamWithTools(@RequestParam String msg,
                                            @RequestParam(required = false) String userId,
                                            @RequestParam(required = false) String conversationId,
                                            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return resolveStreamChat(msg, userId, conversationId, authorization);
    }

    private Flux<String> resolveStreamChat(String msg,
                                           String userId,
                                           String conversationId,
                                           String authorization) {
        String token = AuthTokenUtils.extractToken(authorization);
        if (token != null && !token.isBlank()) {
            AppUser user = authService.requireValidUser(token);
            String resolvedConversationId = conversationService.resolveConversationId(user.getUserId(), conversationId);
            AtomicReference<StringBuilder> assistantBuffer = new AtomicReference<>(new StringBuilder());
            return loveService.chatStream(msg, user.getUserId(), resolvedConversationId)
                    .doOnNext(chunk -> assistantBuffer.get().append(chunk))
                    .doOnComplete(() -> conversationService.saveExchange(
                            user.getUserId(),
                            resolvedConversationId,
                            msg,
                            assistantBuffer.get().toString()));
        }
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("请先登录，或提供 userId");
        }
        return loveService.chatStreamGuest(msg, userId);
    }

    private String resolveSyncChat(String msg,
                                   String userId,
                                   String conversationId,
                                   String authorization) {
        String token = AuthTokenUtils.extractToken(authorization);
        if (token != null && !token.isBlank()) {
            AppUser user = authService.requireValidUser(token);
            String resolvedConversationId = conversationService.resolveConversationId(user.getUserId(), conversationId);
            String response = loveService.chatWithMemory(msg, user.getUserId(), resolvedConversationId);
            conversationService.saveExchange(user.getUserId(), resolvedConversationId, msg, response);
            return response;
        }
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("请先登录，或提供 userId");
        }
        return loveService.chatWithMemoryGuest(msg, userId);
    }
}
