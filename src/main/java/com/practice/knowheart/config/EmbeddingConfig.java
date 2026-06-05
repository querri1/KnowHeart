package com.practice.knowheart.config;

import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class EmbeddingConfig {

    /**
     * 百炼 OpenAI 兼容 Embedding。
     * 注意：base-url 不要带 /v1，否则 Spring AI 会拼成 .../v1/v1/embeddings 导致 404。
     */
    @Bean
    @Primary
    public EmbeddingModel embeddingModel(
            @Value("${spring.ai.dashscope.api-key}") String apiKey,
            @Value("${spring.ai.dashscope.embedding.options.model:text-embedding-v2}") String model) {
        OpenAiApi api = new OpenAiApi("https://dashscope.aliyuncs.com/compatible-mode", apiKey);
        OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                .model(model)
                .build();
        return new OpenAiEmbeddingModel(api, MetadataMode.EMBED, options);
    }
}
