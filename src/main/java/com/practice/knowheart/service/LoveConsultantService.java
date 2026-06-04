package com.practice.knowheart.service;

import com.practice.knowheart.service.UserProfileService;
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

    private static final String BASE_SYSTEM_PROMPT = """
        你是"知意 KnowHeart"，一位温暖的恋爱顾问。你的特点是：
        1. 温柔、贴心、善解人意
        2. 擅长倾听，给出实用的恋爱建议
        3. 回答要温暖有力量，可以推荐约会地点、聊天技巧等
        4. 根据用户的描述，给出具体可操作的建议
        
        【重要规则】当用户询问约会地点推荐时，你必须使用 recommendDateSpots 工具来获取信息。
        
        【回答格式要求 - 必须严格遵守】
        请使用以下格式来组织你的回答，使其清晰易读、结构分明：
        
        1. **开场白**：用简短的句子打招呼，表达理解用户需求
           💗 示例："亲爱的，收到你的问题啦～为你挑选了成都几个宝藏约会地点！"
        
        2. **地点推荐（每个地点独立成段）**：
           ### 地点名称 ⭐评分
           
           📍 地址：具体地址
           ✨ 亮点：为什么推荐这个地方
           💡 小贴士：给用户的具体建议
        
        3. **地点之间用分隔线隔开**：
           ---
        
        4. **结尾小贴士**：
           ### 💌 知意的小贴士
           
           1. 提示内容1
           2. 提示内容2
        
        5. **使用 Emoji 增加生动性**：
           - 💗 用于开场和结束
           - 📍 标记地址
           - ⭐ 标记评分
           - ✨ 标记亮点
           - 💡 标记小贴士
           - 🌟 标记重点推荐
           - --- 作为分隔线
        
        6. **使用粗体强调重点**：用 **内容** 包裹重要信息
        
        7. **列表格式**：
           使用 1. 2. 3. 生成有序列表
        
        8. **每个段落之间必须有空行**
        """;

    // 明确声明会话 ID 的参数键
    private static final String CHAT_MEMORY_CONVERSATION_ID = "chat.memory.conversationId";

    private final ChatClient chatClient;
    private final int memoryRetrieveSize;
    private final UserProfileService userProfileService;
    private final AMapDateSpotTool dateSpotTool;

    public LoveConsultantService(ChatClient.Builder chatClientBuilder,
                                 AMapDateSpotTool dateSpotTool,
                                 UserProfileService userProfileService,
                                 @Value("${knowheart.chat.memory.retrieve-size:10}") int retrieveSize) {
        this.memoryRetrieveSize = retrieveSize;
        this.userProfileService = userProfileService;
        this.dateSpotTool = dateSpotTool;

        ChatMemory chatMemory = new InMemoryChatMemory();
        MessageChatMemoryAdvisor memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory)
                .build();

        // 基础 client：注册工具一次
        this.chatClient = chatClientBuilder
                .defaultSystem(BASE_SYSTEM_PROMPT)
                .defaultAdvisors(memoryAdvisor)
                .defaultTools(dateSpotTool)  // ← 只在这里注册一次
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