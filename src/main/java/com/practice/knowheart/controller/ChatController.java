package com.practice.knowheart.controller;

import com.practice.knowheart.dto.ChatRequest;
import com.practice.knowheart.service.LoveConsultantService;
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