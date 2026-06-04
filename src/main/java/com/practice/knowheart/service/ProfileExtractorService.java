package com.practice.knowheart.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ProfileExtractorService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String EXTRACT_PROMPT = """
        从用户的对话中提取个人信息，返回 JSON 格式。
        
        可提取的字段：name（姓名）、gender（性别）、age（年龄）、city（城市）、relationship（恋爱状态：单身/暗恋/恋爱中/已婚）
        
        用户说：%s
        
        返回 JSON（如果没提到某个字段，就不包含它）：
        """;

    public ProfileExtractorService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * 从消息中提取用户画像
     */
    public UserProfileDto extract(String userMessage, String userId) {
        UserProfileDto dto = new UserProfileDto();
        dto.userId = userId;

        // 方法1：使用正则快速提取（轻量级）
        extractWithRegex(userMessage, dto);

        // 方法2：使用 LLM 提取（更准确，可选的）
        // extractWithLLM(userMessage, dto);

        return dto;
    }

    private void extractWithRegex(String message, UserProfileDto dto) {
        // 姓名提取
        Pattern namePattern = Pattern.compile("(?:我叫|我是|名字叫|姓)([\\u4e00-\\u9fa5]{2,4})");
        Matcher nameMatcher = namePattern.matcher(message);
        if (nameMatcher.find()) {
            dto.name = nameMatcher.group(1);
        }

        // 年龄提取
        Pattern agePattern = Pattern.compile("(\\d{1,3})(?:岁|周岁)");
        Matcher ageMatcher = agePattern.matcher(message);
        if (ageMatcher.find()) {
            dto.age = Integer.parseInt(ageMatcher.group(1));
        }

        // 城市提取
        String[] cities = {"北京", "上海", "广州", "深圳", "杭州", "成都", "重庆", "武汉", "西安", "南京"};
        for (String city : cities) {
            if (message.contains(city)) {
                dto.city = city;
                break;
            }
        }

        // 恋爱状态提取
        if (message.contains("单身")) dto.relationship = "单身";
        else if (message.contains("暗恋")) dto.relationship = "暗恋";
        else if (message.contains("恋爱中") || message.contains("热恋")) dto.relationship = "恋爱中";
        else if (message.contains("已婚")) dto.relationship = "已婚";
    }

    // 数据类
    public static class UserProfileDto {
        public String userId;
        public String name;
        public String gender;
        public Integer age;
        public String city;
        public String relationship;

        public boolean hasAnyInfo() {
            return name != null || gender != null || age != null || city != null || relationship != null;
        }
    }
}