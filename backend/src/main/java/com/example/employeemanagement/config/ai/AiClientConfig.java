package com.example.employeemanagement.config.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the OpenAI-compatible Spring AI clients directly against our own AI_*
 * properties (not spring-ai-openai's autoconfiguration, which expects its own
 * SPRING_AI_OPENAI_* property names). AI_BASE_URL can point at OpenAI itself or
 * any OpenAI-compatible endpoint (Ollama's compat mode, LM Studio, vLLM, a proxy),
 * so AI_PROVIDER is informational/validated rather than selecting a different SDK.
 *
 * All beans are @Conditional on AI being enabled and configured, so the
 * application context still starts cleanly with AI disabled or unconfigured —
 * dependents must inject these via ObjectProvider/Optional.
 */
@Configuration
public class AiClientConfig {

    @Bean
    @Conditional(AiEnabledCondition.class)
    public OpenAiApi openAiApi(AiProperties properties) {
        return OpenAiApi.builder()
                .baseUrl(properties.getBaseUrl())
                .apiKey(properties.getApiKey())
                .build();
    }

    @Bean
    @Conditional(AiEnabledCondition.class)
    public ChatModel chatModel(OpenAiApi openAiApi, AiProperties properties) {
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(properties.getModel())
                .temperature(properties.getTemperature())
                .maxTokens(properties.getMaxTokens())
                .build();
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(options)
                .build();
    }

    @Bean
    @Conditional(AiEnabledCondition.class)
    public EmbeddingModel embeddingModel(OpenAiApi openAiApi) {
        return new OpenAiEmbeddingModel(openAiApi);
    }

    @Bean
    @Conditional(AiEnabledCondition.class)
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }

    @Bean
    @Conditional(AiEnabledCondition.class)
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }
}
