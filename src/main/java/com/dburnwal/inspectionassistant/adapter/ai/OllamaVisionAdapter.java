package com.dburnwal.inspectionassistant.adapter.ai;

import com.dburnwal.inspectionassistant.inspection.domain.Finding;
import com.dburnwal.inspectionassistant.inspection.domain.InspectionGuidance;
import com.dburnwal.inspectionassistant.inspection.domain.Severity;
import com.dburnwal.inspectionassistant.inspection.ports.InspectionProfile;
import com.dburnwal.inspectionassistant.inspection.ports.VisionAnalysisPort;
import com.dburnwal.inspectionassistant.inspection.ports.VisionAnalysisResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.util.MimeTypeUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OllamaVisionAdapter implements VisionAnalysisPort {

    private static final Logger log = LoggerFactory.getLogger(OllamaVisionAdapter.class);

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OllamaVisionAdapter(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @Override
    public VisionAnalysisResult analyze(byte[] imageBytes, InspectionProfile profile) {
        try {
            Media image = Media.builder()
                    .mimeType(MimeTypeUtils.IMAGE_JPEG)
                    .data(imageBytes)
                    .build();
            UserMessage message = UserMessage.builder()
                    .text(profile.buildPrompt())
                    .media(image)
                    .build();

            String response = chatClient.prompt()
                    .messages(message)
                    .call()
                    .content();

            return parse(response, profile);
        } catch (Exception e) {
            log.warn("inspection.ai.analysis.failed reason={}", e.getMessage());
            return new VisionAnalysisResult(List.of(), InspectionGuidance.of("AI analysis temporarily unavailable."));
        }
    }

    private VisionAnalysisResult parse(String json, InspectionProfile profile) {
        try {
            String cleaned = json.replaceAll("(?s)```[a-z]*\\n?", "").replaceAll("```", "").trim();
            JsonNode root = objectMapper.readTree(cleaned);

            List<Finding> findings = new ArrayList<>();
            for (JsonNode node : root.path("findings")) {
                String type = node.path("type").asText("UNKNOWN_DAMAGE");
                if (!profile.getSupportedFindingTypes().contains(type)) {
                    type = "UNKNOWN_DAMAGE";
                }
                Severity severity;
                try {
                    severity = Severity.valueOf(node.path("severity").asText("LOW"));
                } catch (IllegalArgumentException ex) {
                    severity = Severity.LOW;
                }
                findings.add(new Finding(
                        UUID.randomUUID().toString(),
                        type,
                        node.path("part").asText("UNKNOWN"),
                        node.path("description").asText(),
                        node.path("confidence").asDouble(0.5),
                        severity,
                        node.path("requiresCloserInspection").asBoolean(false),
                        node.path("guidanceMessage").asText(""),
                        Instant.now()
                ));
            }

            JsonNode g = root.path("guidance");
            InspectionGuidance guidance = new InspectionGuidance(
                    g.path("action").asText("NONE"),
                    g.path("direction").asText("NONE"),
                    g.path("message").asText("")
            );

            return new VisionAnalysisResult(findings, guidance);
        } catch (Exception e) {
            log.warn("inspection.ai.response.parse.failed reason={}", e.getMessage());
            return new VisionAnalysisResult(List.of(), InspectionGuidance.of(""));
        }
    }
}
