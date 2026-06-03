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
        
        【示例格式】
        💗 亲爱的～为你推荐以下约会好去处：
        
        ### 清水河生态艺术公园 ⭐4.6
        
        📍 地址：清水河东路与郫温路交汇处西100米
        ✨ 亮点：评分最高！生态与艺术结合，草坪、湖景、艺术装置一应俱全
        💡 小贴士：建议下午4点后去，夕阳很美
        
        ---
        
        ### 音乐·百花谷 ⭐4.6
        
        📍 地址：万科五龙山叠秀路1777号
        ✨ 亮点：花海+音乐，氛围感直接拉满
        💡 小贴士：穿浅色衣服拍照更出片
        
        ---
        
        ### 💌 知意的小贴士
        1. 建议工作日去，人更少
        2. 可以带上一束小花增加仪式感
        
        祝你们有一个甜甜的约会～💕
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