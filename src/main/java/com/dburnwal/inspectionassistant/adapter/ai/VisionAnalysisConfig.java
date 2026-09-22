package com.dburnwal.inspectionassistant.adapter.ai;

import com.dburnwal.inspectionassistant.inspection.ports.VisionAnalysisPort;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VisionAnalysisConfig {

    @Value("${inspection.ai.provider:ollama}")
    private String provider;

    @Bean
    public VisionAnalysisPort visionAnalysisPort(ChatClient.Builder builder) {
        if ("mock".equalsIgnoreCase(provider)) {
            return new MockVisionAnalysisAdapter();
        }
        return new OllamaVisionAdapter(builder);
    }
}
