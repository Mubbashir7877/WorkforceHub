package com.example.employeemanagement.config.ai;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Gates creation of the AI provider beans (ChatModel, EmbeddingModel, ChatClient,
 * VectorStore). Reads directly from the Environment rather than the AiProperties
 * bean, since conditions run during bean-definition evaluation, before
 * @ConfigurationProperties beans exist. Keeping this bean-creation gate here (rather
 * than only null-checking later) is what lets the application start normally when
 * AI is disabled or misconfigured, instead of failing to boot.
 */
public class AiEnabledCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Environment env = context.getEnvironment();
        boolean enabled = env.getProperty("ai.enabled", Boolean.class, false);
        String apiKey = env.getProperty("ai.api-key", "");
        return enabled && apiKey != null && !apiKey.isBlank();
    }
}
