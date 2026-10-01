package com.example.aiapp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Single OpenAI client shared by the AI Document Q&A pipeline (embeddings, QA graph)
 * and the EMS AI assistant prompts - replaces the old FastAPI proxy for both.
 */
@Configuration
public class OpenAiConfig {

    @Bean
    public OpenAIClient openAIClient(@Value("${app.openai.api-key}") String apiKey) {
        return OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .build();
    }

    /**
     * A Jackson 2 ObjectMapper, distinct from Spring Boot 4's own Jackson 3
     * auto-configured mapper (com.fasterxml vs. tools.jackson - different packages,
     * so both coexist on the classpath without conflict). The openai-java SDK's raw
     * JSON responses (classifier output, tool-call arguments) are parsed with this
     * one, since openai-java itself depends on Jackson 2.
     */
    @Bean
    public ObjectMapper openAiObjectMapper() {
        return new ObjectMapper();
    }
}
