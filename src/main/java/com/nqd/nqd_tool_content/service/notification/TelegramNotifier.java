package com.nqd.nqd_tool_content.service.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Component
public class TelegramNotifier implements Notifier {

    @Value("${notification.telegram.bot-token:}")
    private String botToken;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String getChannelName() {
        return "TELEGRAM";
    }

    @Override
    public boolean send(String recipientChatId, String title, String body) {
        if (botToken == null || botToken.isBlank()) {
            log.info("[Telegram Dry-Run] Token missing. Recipient: {}, Title: {}, Body: {}", recipientChatId, title, body);
            return true;
        }

        if (recipientChatId == null || recipientChatId.isBlank()) {
            log.warn("Telegram recipient chat ID is blank, skipping");
            return false;
        }

        try {
            String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";
            String text = "🔔 *" + escapeMarkdown(title) + "*\n\n" + body;

            Map<String, Object> payload = Map.of(
                    "chat_id", recipientChatId,
                    "text", text,
                    "parse_mode", "Markdown"
            );

            ResponseEntity<String> res = restTemplate.postForEntity(url, payload, String.class);
            return res.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("Failed to send Telegram notification to {}: {}", recipientChatId, e.getMessage());
            return false;
        }
    }

    private String escapeMarkdown(String text) {
        if (text == null) return "";
        return text.replace("_", "\\_").replace("*", "\\*").replace("[", "\\[").replace("`", "\\`");
    }
}
