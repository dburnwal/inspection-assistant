package com.dburnwal.inspectionassistant.vision;

import com.dburnwal.inspectionassistant.ai.InspectionPromptService;
import com.dburnwal.inspectionassistant.inspection.Finding;
import com.dburnwal.inspectionassistant.inspection.FindingType;
import com.dburnwal.inspectionassistant.inspection.Severity;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class OllamaVisionAnalyzer implements VisionAnalyzer {

    private final ChatClient chatClient;
    private final InspectionPromptService promptService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OllamaVisionAnalyzer(ChatClient.Builder builder, InspectionPromptService promptService) {
        this.chatClient = builder.build();
        this.promptService = promptService;
    }

    @Override
    public List<Finding> analyze(byte[] imageBytes) {
        try {
            Media image = Media.builder()
                    .mimeType(MimeTypeUtils.IMAGE_JPEG)
                    .data(imageBytes)
                    .build();
            UserMessage message = UserMessage.builder()
                    .text(promptService.buildPrompt())
                    .media(image)
                    .build();

            String response = chatClient.prompt()
                    .messages(message)
                    .call()
                    .content();

            return parseFindings(response);
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<Finding> parseFindings(String json) {
        try {
            // Strip markdown code fences if present
            String cleaned = json.replaceAll("(?s)```[a-z]*\\n?", "").replaceAll("```", "").trim();
            JsonNode root = objectMapper.readTree(cleaned);
            JsonNode findingsNode = root.path("findings");
            List<Finding> findings = new ArrayList<>();
            for (JsonNode node : findingsNode) {
                findings.add(new Finding(
                        UUID.randomUUID().toString(),
                        FindingType.valueOf(node.path("type").asText("OTHER")),
                        node.path("description").asText(),
                        node.path("confidence").asDouble(0.5),
                        Severity.valueOf(node.path("severity").asText("LOW")),
                        node.path("action").asText()
                ));
            }
            return findings;
        } catch (Exception e) {
            return List.of();
        }
    }
}
