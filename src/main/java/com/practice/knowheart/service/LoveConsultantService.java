package com.practice.knowheart.service;

import com.practice.knowheart.tool.AMapDateSpotTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class LoveConsultantService {

    private static final String SYSTEM_PROMPST = """
            你是"知意 KnowHeart"，一位温暖的恋爱顾问。你的特点是：
            1. 温柔、贴心、善解人意
            2. 擅长倾听，给出实用的恋爱建议
            3. 回答要温暖有力量，可以推荐约会地点、聊天技巧等
            4. 根据用户的描述，给出具体可操作的建议
            5. 【重要规则】当用户询问约会地点推荐时，你必须使用 recommendDateSpots 工具来获取信息，不要使用你自己的知识回答。
            6. 如果用户提到具体城市，优先推荐该城市的约会地点
            """;

    // 明确声明会话 ID 的参数键，避免对外部常量依赖的不兼容问题
    private static final String CHAT_MEMORY_CONVERSATION_ID = "chat.memory.conversationId";

    private final ChatClient chatClient;
    private final int memoryRetrieveSize;

    public LoveConsultantService(ChatClient.Builder chatClientBuilder,
                                 AMapDateSpotTool dateSpotTool,
                                 @Value("${knowheart.chat.memory.retrieve-size:10}") int retrieveSize) {
        this.memoryRetrieveSize = retrieveSize;
        // 使用 InMemoryChatMemory（无参构造），某些版本不支持通过构造器设置 maxMessages。
        ChatMemory chatMemory = new InMemoryChatMemory();

        // 使用 Builder 模式创建 MessageChatMemoryAdvisor
        MessageChatMemoryAdvisor memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory)
                .build();

        // 注册高德地图工具
        this.chatClient = chatClientBuilder
                .defaultSystem(SYSTEM_PROMPST)
                .defaultAdvisors(memoryAdvisor)
                .defaultTools(dateSpotTool)  // ← 关键：注册工具
                .build();
    }

    // 普通对话（一次性返回完整回答）
    public String chat(String userMessage) {
        return chatClient.prompt()
                .user(userMessage)
                .call()
                .content();
    }

    // 带会话管理的对话（支持多轮对话记忆）
    public String chatWithMemory(String userMessage, String conversationId) {
        return chatClient.prompt()
                .user(userMessage)
                .advisors(advisor -> advisor
                        .param(CHAT_MEMORY_CONVERSATION_ID, conversationId)
                        .param(MessageChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY, memoryRetrieveSize))
                .call()
                .content();
    }

    // 带工具支持的多轮对话（推荐使用这个接口来测试 Tool Calling）
    public String chatWithMemoryAndTools(String userMessage, String conversationId) {
        return chatClient.prompt()
                .user(userMessage)
                .advisors(advisor -> advisor
                        .param(CHAT_MEMORY_CONVERSATION_ID, conversationId)
                        .param(MessageChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY, memoryRetrieveSize))
                .call()
                .content();
    }

    // 流式对话（打字机效果）
    public Flux<String> chatStream(String userMessage, String conversationId) {
        return chatClient.prompt()
                .user(userMessage)
                .advisors(advisor -> advisor
                        .param(CHAT_MEMORY_CONVERSATION_ID, conversationId))
                .stream()
                .content();
    }

    // 带工具支持的流式对话
    public Flux<String> chatStreamWithTools(String userMessage, String conversationId) {
        return chatClient.prompt()
                .user(userMessage)
                .advisors(advisor -> advisor
                        .param(CHAT_MEMORY_CONVERSATION_ID, conversationId))
                .stream()
                .content();
    }
}