package com.practice.knowheart.controller;

import com.practice.knowheart.dto.ChatRequest;
import com.practice.knowheart.service.LoveConsultantService;
import com.practice.knowheart.service.UserProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/knowheart")
public class ChatController {

    private final LoveConsultantService loveService;

    public ChatController(LoveConsultantService loveService) {
        this.loveService = loveService;
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
    public String chatWithMemory(@RequestParam String msg, @RequestParam String userId) {
        return loveService.chatWithMemory(msg, userId);
    }

    @GetMapping(value = "/chat/stream", produces = "text/event-stream")
    public Flux<String> chatStream(@RequestParam String msg, @RequestParam String userId) {
        return loveService.chatStream(msg, userId);
    }

    @PostMapping("/chat")
    public String chatPost(@RequestBody ChatRequest request) {
        return loveService.chatWithMemory(request.getMessage(), request.getUserId());
    }

    @Autowired
    private UserProfileService userProfileService;

    // 获取用户画像
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@RequestParam String userId) {
        var profile = userProfileService.getProfile(userId);
        if (profile.isPresent()) {
            return ResponseEntity.ok(profile.get());
        }
        return ResponseEntity.ok("暂无画像数据，请先发送一些消息");
    }

    // 带 RAG 知识库的对话
    @GetMapping("/chat/rag")
    public String chatWithRag(@RequestParam String msg, @RequestParam String userId) {
        return loveService.chatWithRag(msg, userId);
    }

    @GetMapping(value = "/chat/stream/rag", produces = "text/event-stream")
    public Flux<String> chatStreamWithRag(@RequestParam String msg, @RequestParam String userId) {
        return loveService.chatStreamWithRag(msg, userId);
    }

    // 带工具支持的多轮对话
    @GetMapping("/chat/tools")
    public String chatWithTools(@RequestParam String msg, @RequestParam String userId) {
        return loveService.chatWithMemoryAndTools(msg, userId);
    }

    // 带工具支持的流式对话
    @GetMapping(value = "/chat/stream/tools", produces = "text/event-stream")
    public Flux<String> chatStreamWithTools(@RequestParam String msg, @RequestParam String userId) {
        return loveService.chatStreamWithTools(msg, userId);
    }
}