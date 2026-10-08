package com.nqd.nqd_tool_content.service.ai.impl;

import com.nqd.nqd_tool_content.service.ai.AIProvider;
import com.nqd.nqd_tool_content.service.ai.PromptBuilder;
import com.nqd.nqd_tool_content.service.ai.dto.AIContentRequest;
import com.nqd.nqd_tool_content.service.ai.dto.AIContentResponse;
import com.nqd.nqd_tool_content.service.ai.dto.GeneratedPlatformPost;
import com.nqd.nqd_tool_content.service.ai.dto.PlanAnglesRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class GeminiProviderImpl implements AIProvider {

    private final FakeAIProvider fakeAIProvider;
    private final PromptBuilder promptBuilder;
    private final RestClient restClient;

    public GeminiProviderImpl(FakeAIProvider fakeAIProvider, PromptBuilder promptBuilder) {
        this.fakeAIProvider = fakeAIProvider;
        this.promptBuilder = promptBuilder;
        
        var requestFactory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(java.time.Duration.ofSeconds(10));
        requestFactory.setReadTimeout(java.time.Duration.ofSeconds(20));
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    @Value("${ai.gemini.api-key:}")
    private String apiKey;

    @Value("${ai.gemini.model:gemini-3.8-flash}")
    private String model;

    @Override
    public String getProviderName() {
        return "GEMINI";
    }

    @Override
    @SuppressWarnings("unchecked")
    public AIContentResponse generateContent(AIContentRequest request) {
        if (apiKey == null || apiKey.isBlank()) {
            log.info("[GeminiProviderImpl] No API key configured, falling back to FakeAIProvider");
            AIContentResponse res = fakeAIProvider.generateContent(request);
            res.setProviderUsed(getProviderName());
            res.setModelUsed(model);
            return res;
        }

        String prompt = promptBuilder.buildPostGenerationPrompt(request);

        // Danh sách model Google Gemini hoạt động ổn định & mới nhất
        List<String> modelsToTry = List.of(
                "gemini-3.5-flash-lite",
                "gemini-3.1-flash-lite",
                "gemini-3.7-flash",
                "gemini-3.6-flash",
                model
        );

        tools.jackson.databind.ObjectMapper mapper = new tools.jackson.databind.ObjectMapper();

        for (String m : modelsToTry) {
            try {
                String modelPath = m.startsWith("models/") ? m : "models/" + m;
                String url = "https://generativelanguage.googleapis.com/v1beta/" + modelPath + ":generateContent?key=" + apiKey;

                Map<String, Object> requestBody = Map.of(
                        "contents", List.of(
                                Map.of("parts", List.of(Map.of("text", prompt)))
                        ),
                        "generationConfig", Map.of(
                                "temperature", 0.7,
                                "responseMimeType", "application/json"
                        )
                );

                String rawResponse = restClient.post()
                        .uri(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON, MediaType.ALL)
                        .body(requestBody)
                        .retrieve()
                        .body(String.class);

                if (rawResponse != null && !rawResponse.isBlank()) {
                    var rootNode = mapper.readTree(rawResponse);
                    var candidatesNode = rootNode.path("candidates");
                    if (candidatesNode.isArray() && !candidatesNode.isEmpty()) {
                        var firstCandidate = candidatesNode.get(0);
                        var partsNode = firstCandidate.path("content").path("parts");
                        if (partsNode.isArray() && !partsNode.isEmpty()) {
                            String rawJson = partsNode.get(0).path("text").asText();
                            if (rawJson != null && !rawJson.isBlank()) {
                                rawJson = cleanJsonString(rawJson);
                                AIContentResponse res = parseGeminiJsonWithMapper(rawJson, rootNode, m, mapper);
                                if (res != null && !res.getPosts().isEmpty() && res.getPosts().get(0).getContent() != null && !res.getPosts().get(0).getContent().isBlank()) {
                                    log.info("[GeminiProviderImpl] Successfully generated content using model: {}", m);
                                    return res;
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("[GeminiProviderImpl] Model {} failed ({}). Trying next candidate...", m, e.getMessage());
            }
        }

        // Fallback an toàn nếu có sự cố mạng hoặc lỗi quota
        AIContentResponse fallbackRes = fakeAIProvider.generateContent(request);
        fallbackRes.setProviderUsed(getProviderName());
        fallbackRes.setModelUsed(model);
        return fallbackRes;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> generatePlanAngles(PlanAnglesRequest request) {
        if (apiKey == null || apiKey.isBlank()) {
            return fakeAIProvider.generatePlanAngles(request);
        }

        log.info("[GeminiProviderImpl] Generating plan angles with Gemini ({}) for topic: {}", model, request.getTopic());
        String prompt = promptBuilder.buildPlanAnglesPrompt(request.getTopic(), request.getCount(), request.getLanguage());

        for (String m : List.of("gemini-3.5-flash-lite", "gemini-3.1-flash-lite", "gemini-3.7-flash", "gemini-3.6-flash")) {
            try {
                String modelPath = m.startsWith("models/") ? m : "models/" + m;
                String url = "https://generativelanguage.googleapis.com/v1beta/" + modelPath + ":generateContent?key=" + apiKey;
                Map<String, Object> requestBody = Map.of(
                        "contents", List.of(
                                Map.of("parts", List.of(Map.of("text", prompt)))
                        ),
                        "generationConfig", Map.of(
                                "temperature", 0.8,
                                "responseMimeType", "application/json"
                        )
                );

                String rawResponse = restClient.post()
                        .uri(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON, MediaType.ALL)
                        .body(requestBody)
                        .retrieve()
                        .body(String.class);

                if (rawResponse != null && !rawResponse.isBlank()) {
                    tools.jackson.databind.ObjectMapper mapper = new tools.jackson.databind.ObjectMapper();
                    var rootNode = mapper.readTree(rawResponse);
                    var candidatesNode = rootNode.path("candidates");
                    if (candidatesNode.isArray() && !candidatesNode.isEmpty()) {
                        var partsNode = candidatesNode.get(0).path("content").path("parts");
                        if (partsNode.isArray() && !partsNode.isEmpty()) {
                            String rawJson = partsNode.get(0).path("text").asText();
                            if (rawJson != null && !rawJson.isBlank()) {
                                rawJson = cleanJsonString(rawJson);
                                List<String> parsedAngles = parseAnglesJson(rawJson);
                                if (!parsedAngles.isEmpty()) {
                                    return parsedAngles;
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("[GeminiProviderImpl] Error generating plan angles via model {}: {}. Trying next...", m, e.getMessage());
            }
        }

        return fakeAIProvider.generatePlanAngles(request);
    }

    @Override
    public com.nqd.nqd_tool_content.dto.response.PostChatResponse chat(
            com.nqd.nqd_tool_content.dto.request.PostChatRequest request,
            String topic,
            String angle,
            String tone,
            String platform
    ) {
        if (apiKey == null || apiKey.isBlank()) {
            return com.nqd.nqd_tool_content.dto.response.PostChatResponse.builder()
                    .reply("Chào bạn! Hãy cấu hình Gemini API Key để trò chuyện trực tiếp cùng AI nhé.")
                    .suggestedContent(request.getCurrentContent())
                    .modelUsed("mock")
                    .build();
        }

        String prompt = promptBuilder.buildPostChatPrompt(
                request.getMessage(),
                request.getCurrentContent(),
                platform,
                topic,
                angle,
                tone,
                request.getHistory()
        );

        tools.jackson.databind.ObjectMapper mapper = new tools.jackson.databind.ObjectMapper();

        for (String m : List.of("gemini-3.5-flash-lite", "gemini-3.1-flash-lite", "gemini-3.7-flash", "gemini-3.6-flash")) {
            try {
                String modelPath = m.startsWith("models/") ? m : "models/" + m;
                String url = "https://generativelanguage.googleapis.com/v1beta/" + modelPath + ":generateContent?key=" + apiKey;

                List<Map<String, Object>> parts = new ArrayList<>();
                parts.add(Map.of("text", prompt));

                if (request.getAttachments() != null && !request.getAttachments().isEmpty()) {
                    for (var att : request.getAttachments()) {
                        if (att.getBase64Data() == null || att.getBase64Data().isBlank()) continue;
                        String cleanData = att.getBase64Data();
                        if (cleanData.contains(",")) {
                            cleanData = cleanData.substring(cleanData.indexOf(",") + 1);
                        }
                        cleanData = cleanData.trim();
                        String mime = att.getMimeType() != null ? att.getMimeType().toLowerCase() : "";

                        if (mime.startsWith("image/") || mime.equals("application/pdf")) {
                            parts.add(Map.of(
                                    "inlineData", Map.of(
                                            "mimeType", !mime.isBlank() ? mime : "image/jpeg",
                                            "data", cleanData
                                    )
                            ));
                        } else {
                            try {
                                byte[] decoded = java.util.Base64.getDecoder().decode(cleanData);
                                String textContent = new String(decoded, java.nio.charset.StandardCharsets.UTF_8);
                                parts.add(Map.of(
                                        "text", "--- NỘI DUNG TỆP ĐÍNH KÈM (" + (att.getFileName() != null ? att.getFileName() : "Tệp") + ") ---\n" + textContent + "\n--- HẾT TỆP ---"
                                ));
                            } catch (Exception ex) {
                                log.warn("[GeminiProviderImpl] Failed to decode text attachment {}: {}", att.getFileName(), ex.getMessage());
                            }
                        }
                    }
                }

                Map<String, Object> requestBody = Map.of(
                        "contents", List.of(
                                Map.of("parts", parts)
                        ),
                        "generationConfig", Map.of(
                                "temperature", 0.7,
                                "responseMimeType", "application/json"
                        )
                );

                String rawResponse = restClient.post()
                        .uri(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON, MediaType.ALL)
                        .body(requestBody)
                        .retrieve()
                        .body(String.class);

                if (rawResponse != null && !rawResponse.isBlank()) {
                    var rootNode = mapper.readTree(rawResponse);
                    var candidatesNode = rootNode.path("candidates");
                    if (candidatesNode.isArray() && !candidatesNode.isEmpty()) {
                        var partsNode = candidatesNode.get(0).path("content").path("parts");
                        if (partsNode.isArray() && !partsNode.isEmpty()) {
                            String rawJson = partsNode.get(0).path("text").asText();
                            if (rawJson != null && !rawJson.isBlank()) {
                                rawJson = cleanJsonString(rawJson);
                                var jsonNode = mapper.readTree(rawJson);
                                String reply = jsonNode.path("reply").asText();
                                String suggestedContent = jsonNode.hasNonNull("suggestedContent") && !jsonNode.path("suggestedContent").isNull()
                                        ? jsonNode.path("suggestedContent").asText() : null;
                                if ("null".equalsIgnoreCase(suggestedContent) || (suggestedContent != null && suggestedContent.isBlank())) {
                                    suggestedContent = null;
                                }

                                List<String> hashtags = new ArrayList<>();
                                var tagsNode = jsonNode.path("suggestedHashtags");
                                if (tagsNode.isArray()) {
                                    for (var t : tagsNode) hashtags.add(t.asText());
                                }

                                return com.nqd.nqd_tool_content.dto.response.PostChatResponse.builder()
                                        .reply(reply != null && !reply.isBlank() ? reply : "Đã cập nhật bài viết theo ý bạn.")
                                        .suggestedContent(suggestedContent)
                                        .suggestedHashtags(hashtags)
                                        .modelUsed(m)
                                        .build();
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("[GeminiProviderImpl] Chat failed with model {}: {}", m, e.getMessage());
            }
        }

        return com.nqd.nqd_tool_content.dto.response.PostChatResponse.builder()
                .reply("Hiện tại kết nối AI đang bận. Bạn vui lòng thử lại sau giây lát nhé.")
                .suggestedContent(request.getCurrentContent())
                .modelUsed("fallback")
                .build();
    }

    private String cleanJsonString(String raw) {
        String cleaned = raw.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        }
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        return cleaned.trim();
    }

    private AIContentResponse parseGeminiJsonWithMapper(
            String rawJson,
            tools.jackson.databind.JsonNode geminiRoot,
            String modelUsed,
            tools.jackson.databind.ObjectMapper mapper
    ) {
        List<GeneratedPlatformPost> posts = new ArrayList<>();
        String imagePrompt = "";

        try {
            var jsonNode = mapper.readTree(rawJson);
            tools.jackson.databind.JsonNode itemsNode = null;
            if (jsonNode.has("items")) {
                itemsNode = jsonNode.get("items");
            } else if (jsonNode.has("posts")) {
                itemsNode = jsonNode.get("posts");
            } else if (jsonNode.isArray()) {
                itemsNode = jsonNode;
            }

            if (itemsNode != null && itemsNode.isArray()) {
                for (var item : itemsNode) {
                    String platform = item.path("platform").asText("FACEBOOK").toUpperCase();
                    String content = item.path("content").asText("");
                    List<String> hashtags = new ArrayList<>();
                    var tagsNode = item.path("hashtags");
                    if (tagsNode.isArray()) {
                        for (var t : tagsNode) {
                            String tag = t.asText().trim();
                            if (!tag.isEmpty()) hashtags.add(tag);
                        }
                    }
                    if (!content.isBlank()) {
                        posts.add(GeneratedPlatformPost.builder()
                                .platform(platform)
                                .content(content)
                                .hashtags(hashtags)
                                .build());
                    }
                }
            }

            if (jsonNode.has("imagePrompt")) {
                imagePrompt = jsonNode.get("imagePrompt").asText("");
            }
        } catch (Exception e) {
            log.warn("[GeminiProviderImpl] Failed to parse JSON with ObjectMapper: {}, trying fallback parser", e.getMessage());
            return parseGeminiJsonResponse(rawJson, Map.of());
        }

        int promptTokens = 150;
        int completionTokens = 300;
        var usage = geminiRoot.path("usageMetadata");
        if (!usage.isMissingNode()) {
            if (usage.has("promptTokenCount")) promptTokens = usage.get("promptTokenCount").asInt();
            if (usage.has("candidatesTokenCount")) completionTokens = usage.get("candidatesTokenCount").asInt();
        }

        return AIContentResponse.builder()
                .posts(posts)
                .suggestedImagePrompt(imagePrompt != null && !imagePrompt.isBlank() ? imagePrompt : "A modern creative digital illustration")
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .estimatedCostUsd(new BigDecimal("0.0015"))
                .modelUsed(modelUsed)
                .providerUsed(getProviderName())
                .build();
    }

    @SuppressWarnings("unchecked")
    private AIContentResponse parseGeminiJsonResponse(String rawJson, Map<String, Object> geminiResponse) {
        List<GeneratedPlatformPost> posts = new ArrayList<>();
        String imagePrompt = "";

        try {
            // Đọc thủ công hoặc thông qua helper đơn giản để không phụ thuộc external Jackson
            // Định dạng: {"items": [{"platform": "FACEBOOK", "content": "...", "hashtags": [...]}]}
            if (rawJson.contains("\"items\"")) {
                // Tách các item bằng substring / regex đơn giản an toàn
                String[] segments = rawJson.split("\\{\\s*\"platform\"");
                for (int i = 1; i < segments.length; i++) {
                    String seg = segments[i];
                    String platform = extractJsonField(seg, "platform");
                    String content = extractJsonField(seg, "content");
                    List<String> hashtags = extractHashtags(seg);

                    if (platform != null && content != null) {
                        posts.add(GeneratedPlatformPost.builder()
                                .platform(platform.toUpperCase())
                                .content(unescapeJson(content))
                                .hashtags(hashtags)
                                .build());
                    }
                }
            }

            int imgPromptIdx = rawJson.indexOf("\"imagePrompt\"");
            if (imgPromptIdx != -1) {
                String sub = rawJson.substring(imgPromptIdx);
                imagePrompt = extractJsonField(sub, "imagePrompt");
                if (imagePrompt != null) {
                    imagePrompt = unescapeJson(imagePrompt);
                }
            }
        } catch (Exception e) {
            log.warn("[GeminiProviderImpl] Error parsing JSON text: {}", e.getMessage());
        }

        int promptTokens = 150;
        int completionTokens = 300;
        if (geminiResponse.containsKey("usageMetadata")) {
            Map<String, Object> usage = (Map<String, Object>) geminiResponse.get("usageMetadata");
            if (usage != null) {
                if (usage.containsKey("promptTokenCount")) {
                    promptTokens = ((Number) usage.get("promptTokenCount")).intValue();
                }
                if (usage.containsKey("candidatesTokenCount")) {
                    completionTokens = ((Number) usage.get("candidatesTokenCount")).intValue();
                }
            }
        }

        return AIContentResponse.builder()
                .posts(posts)
                .suggestedImagePrompt(imagePrompt != null ? imagePrompt : "A modern creative digital illustration")
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .estimatedCostUsd(new BigDecimal("0.0015"))
                .modelUsed(model)
                .providerUsed(getProviderName())
                .build();
    }

    private List<String> parseAnglesJson(String rawJson) {
        List<String> list = new ArrayList<>();
        try {
            String[] parts = rawJson.split("\"");
            for (int i = 1; i < parts.length; i += 2) {
                String val = parts[i].trim();
                if (!val.equals("[") && !val.equals("]") && !val.equals(",") && !val.isBlank()) {
                    list.add(unescapeJson(val));
                }
            }
        } catch (Exception e) {
            log.warn("Error parsing angles: {}", e.getMessage());
        }
        return list;
    }

    private String extractJsonField(String source, String field) {
        String target = "\"" + field + "\"";
        int idx = source.indexOf(target);
        if (idx == -1) {
            target = field;
            idx = source.indexOf(target);
            if (idx == -1) return null;
        }

        int colonIdx = source.indexOf(":", idx + target.length());
        if (colonIdx == -1) return null;

        int firstQuote = source.indexOf("\"", colonIdx + 1);
        if (firstQuote == -1) return null;

        StringBuilder sb = new StringBuilder();
        boolean escaped = false;
        for (int i = firstQuote + 1; i < source.length(); i++) {
            char c = source.charAt(i);
            if (escaped) {
                sb.append(c);
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                return sb.toString();
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private List<String> extractHashtags(String source) {
        List<String> tags = new ArrayList<>();
        int hashIdx = source.indexOf("\"hashtags\"");
        if (hashIdx == -1) return tags;

        int startBracket = source.indexOf("[", hashIdx);
        int endBracket = source.indexOf("]", startBracket != -1 ? startBracket : hashIdx);
        if (startBracket != -1 && endBracket != -1) {
            String arrayContent = source.substring(startBracket + 1, endBracket);
            String[] items = arrayContent.split(",");
            for (String item : items) {
                String tag = item.replace("\"", "").replace("'", "").trim();
                if (!tag.isEmpty()) {
                    tags.add(tag);
                }
            }
        }
        return tags;
    }

    private String unescapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\n", "\n")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\t", "\t");
    }
}
