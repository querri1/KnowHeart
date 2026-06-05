package com.practice.knowheart.service;

import com.practice.knowheart.tool.AMapDateSpotTool;
import com.practice.knowheart.tool.WeatherTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
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
        3. 回答恋爱问题时，优先使用下方注入的参考资料内容作答，自然融入回答，不要提及「知识库」「检索」；参考资料有相关内容时不得声称「没有相关信息」
        
        【回答格式要求 - 必须严格遵守】
        1. 使用纯文本 + Emoji 组织内容；禁止 #、```、- 列表符号等 Markdown 语法
        2. 允许且必须：用 **加粗** 标记每条建议/每个板块的小标题（如 **先倾听，别急着给建议**），小标题单独一行，便于阅读
        3. 每个段落、每个地点推荐之间必须用「空行」分隔（输出真实换行符，不要挤在一段里）
        4. 一般回答示例结构：
        
        💗 开场白（1-2 句）
        
        💗 **小标题一**
        具体建议内容...
        
        💗 **小标题二**
        具体建议内容...
        
        💕 温暖结尾
        
        5. 推荐地点时使用如下结构（每行单独一行，地点名可加粗）：
        
        💗 开场白
        
        **地点名称**（评分 x.x）
        📍 地址：xxx
        ✨ 亮点：xxx
        💡 小贴士：xxx
        
        ---
        
        💌 **知意的小贴士**
        1. xxx
        2. xxx
        
        6. 禁止用 ``` 代码块包裹回答
        """;

    private static final String CHAT_MEMORY_CONVERSATION_ID = "chat.memory.conversationId";

    private final ChatClient chatClient;
    private final int memoryRetrieveSize;
    private final UserProfileService userProfileService;
    private final QuestionAnswerAdvisor questionAnswerAdvisor;

    public LoveConsultantService(ChatClient.Builder chatClientBuilder,
                                 AMapDateSpotTool dateSpotTool,
                                 WeatherTool weatherTool,
                                 UserProfileService userProfileService,
                                 QuestionAnswerAdvisor questionAnswerAdvisor,
                                 @Value("${knowheart.chat.memory.retrieve-size:10}") int retrieveSize) {
        this.memoryRetrieveSize = retrieveSize;
        this.userProfileService = userProfileService;
        this.questionAnswerAdvisor = questionAnswerAdvisor;

        ChatMemory chatMemory = new InMemoryChatMemory();
        MessageChatMemoryAdvisor memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory)
                .build();

        // 默认启用：多轮记忆 + RAG 知识库 + 工具
        this.chatClient = chatClientBuilder
                .defaultSystem(BASE_SYSTEM_PROMPT)
                .defaultAdvisors(memoryAdvisor, questionAnswerAdvisor)
                .defaultTools(dateSpotTool, weatherTool)
                .build();
    }

    private String buildSystemPromptWithProfile(String conversationId) {
        String profileSummary = userProfileService.getProfileSummary(conversationId);
        if (profileSummary != null && !profileSummary.isEmpty()) {
            return BASE_SYSTEM_PROMPT + "\n\n" + profileSummary;
        }
        return BASE_SYSTEM_PROMPT;
    }

    private ChatClient createClientWithProfile(String conversationId) {
        String enhancedPrompt = buildSystemPromptWithProfile(conversationId);
        if (enhancedPrompt.equals(BASE_SYSTEM_PROMPT)) {
            return chatClient;
        }
        return chatClient.mutate()
                .defaultSystem(enhancedPrompt)
                .build();
    }

    private ChatClient.ChatClientRequestSpec applyConversationAdvisors(
            ChatClient.ChatClientRequestSpec spec, String conversationId) {
        return spec.advisors(advisor -> advisor
                .param(CHAT_MEMORY_CONVERSATION_ID, conversationId)
                .param(MessageChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY, memoryRetrieveSize));
    }

    // 普通对话（无记忆、无 RAG）
    public String chat(String userMessage) {
        return chatClient.prompt()
                .user(userMessage)
                .call()
                .content();
    }

    // 带 RAG 知识库的对话（同步）
    public String chatWithRag(String userMessage, String conversationId) {
        return chatWithMemory(userMessage, conversationId);
    }

    // 多轮记忆 + 用户画像 + RAG + 工具
    public String chatWithMemory(String userMessage, String conversationId) {
        userProfileService.updateFromMessage(conversationId, userMessage);
        ChatClient clientWithProfile = createClientWithProfile(conversationId);

        return applyConversationAdvisors(clientWithProfile.prompt(), conversationId)
                .user(userMessage)
                .call()
                .content();
    }

    public String chatWithMemoryAndTools(String userMessage, String conversationId) {
        return chatWithMemory(userMessage, conversationId);
    }

    // 流式对话（含 RAG）
    public Flux<String> chatStream(String userMessage, String conversationId) {
        userProfileService.updateFromMessage(conversationId, userMessage);
        ChatClient clientWithProfile = createClientWithProfile(conversationId);

        return applyConversationAdvisors(clientWithProfile.prompt(), conversationId)
                .user(userMessage)
                .stream()
                .content();
    }

    public Flux<String> chatStreamWithRag(String userMessage, String conversationId) {
        return chatStream(userMessage, conversationId);
    }

    public Flux<String> chatStreamWithTools(String userMessage, String conversationId) {
        return chatStream(userMessage, conversationId);
    }
}
