package com.practice.knowheart.service;

import com.practice.knowheart.tool.AMapDateSpotTool;
import com.practice.knowheart.tool.WeatherTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class LoveConsultantService {

    private static final String BASE_SYSTEM_PROMPT = """
        你是"知意 KnowHeart"，一位温暖的恋爱顾问。你的特点是：
        1. 温柔、贴心、善解人意
        2. 擅长倾听，给出实用的恋爱建议
        3. 回答要温暖有力量
        
        【重要规则】
        1. 当用户询问约会地点推荐时，你必须使用 recommendDateSpots 工具
        2. 当用户询问天气时，你必须使用 getWeather 工具
        
        【回答格式要求 - 必须严格遵守】
        1. 禁止使用 ``` 代码块包裹你的回答
        2. 禁止在回答中出现独立的 # 符号（Markdown 标题会被前端自动渲染，不需要手动写）
        3. 使用换行和空行来组织内容，不要用多个连续的 # 号
        4. 使用以下纯文本格式，不要使用任何 Markdown 语法：
        
        示例格式（直接输出，不要加代码块）：
        
        💗 亲爱的，为你推荐以下约会好去处：
        
        清水河生态艺术公园 (评分4.6)
        📍 地址：清水河东路与郫温路交汇处西100米
        ✨ 亮点：生态与艺术结合，草坪、湖景一应俱全
        💡 小贴士：建议下午4点后去，夕阳很美
        
        ---
        
        音乐·百花谷 (评分4.6)
        📍 地址：万科五龙山叠秀路1777号
        ✨ 亮点：花海+音乐，氛围感直接拉满
        💡 小贴士：穿浅色衣服拍照更出片
        
        ---
        
        💌 知意的小贴士
        1. 建议工作日去，人更少
        2. 可以带上一束小花增加仪式感
        
        祝你们有一个甜甜的约会～💕
        
        【禁止事项】
        - 禁止使用 ### 作为标题标记
        - 禁止使用 ``` 代码块
        - 禁止使用 ** 粗体标记（前端会自动处理）
        - 直接用换行和 Emoji 来组织内容
        """;

    // 明确声明会话 ID 的参数键
    private static final String CHAT_MEMORY_CONVERSATION_ID = "chat.memory.conversationId";

    private final ChatClient chatClient;
    private final int memoryRetrieveSize;
    private final UserProfileService userProfileService;
    private final AMapDateSpotTool dateSpotTool;
    private final WeatherTool weatherTool;  // ← 添加


    public LoveConsultantService(ChatClient.Builder chatClientBuilder,
                                 AMapDateSpotTool dateSpotTool,
                                 WeatherTool weatherTool,
                                 UserProfileService userProfileService,
                                 @Value("${knowheart.chat.memory.retrieve-size:10}") int retrieveSize) {
        this.memoryRetrieveSize = retrieveSize;
        this.userProfileService = userProfileService;
        this.dateSpotTool = dateSpotTool;
        this.weatherTool = weatherTool;

        ChatMemory chatMemory = new InMemoryChatMemory();
        MessageChatMemoryAdvisor memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory)
                .build();

        // 基础 client：注册工具一次
        this.chatClient = chatClientBuilder
                .defaultSystem(BASE_SYSTEM_PROMPT)
                .defaultAdvisors(memoryAdvisor)
                .defaultTools(dateSpotTool,weatherTool)  // ← 只在这里注册一次
                .build();
    }

    // 获取带用户画像的增强 System Prompt
    private String buildSystemPromptWithProfile(String conversationId) {
        String profileSummary = userProfileService.getProfileSummary(conversationId);
        if (profileSummary != null && !profileSummary.isEmpty()) {
            return BASE_SYSTEM_PROMPT + "\n\n" + profileSummary;
        }
        return BASE_SYSTEM_PROMPT;
    }

    // 创建带用户画像的 ChatClient（不重复注册工具）
    private ChatClient createClientWithProfile(String conversationId) {
        String enhancedPrompt = buildSystemPromptWithProfile(conversationId);

        // 如果 prompt 没变化，直接返回原 client
        if (enhancedPrompt.equals(BASE_SYSTEM_PROMPT)) {
            return chatClient;
        }

        // 创建新的 ChatClient 实例，只修改 System Prompt，不重新注册工具
        return chatClient.mutate()
                .defaultSystem(enhancedPrompt)
                .defaultAdvisors(advisor -> advisor
                        .param(CHAT_MEMORY_CONVERSATION_ID, conversationId)
                        .param(MessageChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY, memoryRetrieveSize))
                // ← 关键：不要在这里添加 .defaultTools()！
                .build();
    }

    // 普通对话
    public String chat(String userMessage) {
        return chatClient.prompt()
                .user(userMessage)
                .call()
                .content();
    }

    // 带会话管理的对话（支持多轮对话记忆 + 用户画像）
    public String chatWithMemory(String userMessage, String conversationId) {
        userProfileService.updateFromMessage(conversationId, userMessage);
        ChatClient clientWithProfile = createClientWithProfile(conversationId);

        return clientWithProfile.prompt()
                .user(userMessage)
                .advisors(advisor -> advisor
                        .param(CHAT_MEMORY_CONVERSATION_ID, conversationId)
                        .param(MessageChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY, memoryRetrieveSize))
                .call()
                .content();
    }

    // 带工具支持的多轮对话
    public String chatWithMemoryAndTools(String userMessage, String conversationId) {
        userProfileService.updateFromMessage(conversationId, userMessage);
        ChatClient clientWithProfile = createClientWithProfile(conversationId);

        return clientWithProfile.prompt()
                .user(userMessage)
                .advisors(advisor -> advisor
                        .param(CHAT_MEMORY_CONVERSATION_ID, conversationId)
                        .param(MessageChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY, memoryRetrieveSize))
                .call()
                .content();
    }

    // 流式对话
    public Flux<String> chatStream(String userMessage, String conversationId) {
        userProfileService.updateFromMessage(conversationId, userMessage);
        ChatClient clientWithProfile = createClientWithProfile(conversationId);

        return clientWithProfile.prompt()
                .user(userMessage)
                .advisors(advisor -> advisor
                        .param(CHAT_MEMORY_CONVERSATION_ID, conversationId))
                .stream()
                .content();
    }

    // 带工具支持的流式对话
    public Flux<String> chatStreamWithTools(String userMessage, String conversationId) {
        userProfileService.updateFromMessage(conversationId, userMessage);
        ChatClient clientWithProfile = createClientWithProfile(conversationId);

        return clientWithProfile.prompt()
                .user(userMessage)
                .advisors(advisor -> advisor
                        .param(CHAT_MEMORY_CONVERSATION_ID, conversationId))
                .stream()
                .content();
    }
}