
package com.prodapt.license_tracker_backend.config;

import com.prodapt.license_tracker_backend.ai.LicenseTrackerAssistant;
import com.prodapt.license_tracker_backend.ai.LicenseTrackerTools;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class AiConfig {

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Bean
    public ChatMemoryStore chatMemoryStore() {
        log.info("Initializing In-Memory Chat Memory Store");
        return new InMemoryChatMemoryStore();
    }

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        // Try environment variable first, then application.properties
        String apiKey = geminiApiKey;
        if (apiKey == null || apiKey.trim().isEmpty()) {
            apiKey = System.getenv("GEMINI_API_KEY");
        }

        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new RuntimeException(
                    "Error: GEMINI_API_KEY is not set. " +
                            "Please set it in application.properties as 'gemini.api.key' or " +
                            "as environment variable 'GEMINI_API_KEY'."
            );
        }

        log.info("Initializing Google Gemini Chat Model");
        return GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gemini-2.0-flash")
                .temperature(0.7)
                .maxOutputTokens(2048)
                .build();
    }

    @Bean
    public LicenseTrackerAssistant licenseTrackerAssistant(
            ChatLanguageModel chatLanguageModel,
            ChatMemoryStore chatMemoryStore,
            LicenseTrackerTools licenseTrackerTools) {

        log.info("Building LicenseTrackerAssistant with AI Services");

        return AiServices.builder(LicenseTrackerAssistant.class)
                .chatLanguageModel(chatLanguageModel)
                .chatMemoryProvider(chatId -> MessageWindowChatMemory.builder()
                        .chatMemoryStore(chatMemoryStore)
                        .maxMessages(20)
                        .id(chatId)
                        .build())
                .tools(licenseTrackerTools)
                .build();
    }
}
