package com.practice.knowheart.config;

import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RagAdvisorConfig {

    @Bean
    public QuestionAnswerAdvisor questionAnswerAdvisor(
            @Qualifier("loveVectorStore") VectorStore vectorStore,
            @Value("${knowheart.rag.top-k:6}") int topK,
            @Value("${knowheart.rag.similarity-threshold:0.0}") double similarityThreshold) {
        return QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(SearchRequest.builder()
                        .topK(topK)
                        .similarityThreshold(similarityThreshold)
                        .build())
                .userTextAdvise("""
                        以下是与用户问题相关的参考资料：
                        ---------------------
                        {question_answer_context}
                        ---------------------
                        请结合以上资料回答用户。资料中有相关内容时务必使用并准确作答；
                        仅当资料完全无法回答该问题时，再委婉说明不确定。
                        """)
                .build();
    }
}
