package com.nqd.nqd_tool_content.service.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EmailNotifier implements Notifier {

    @Override
    public String getChannelName() {
        return "EMAIL";
    }

    @Override
    public boolean send(String recipientEmail, String title, String body) {
        if (recipientEmail == null || recipientEmail.isBlank()) {
            return false;
        }
        // Log dry-run email notification
        log.info("[Email Notifier] Sending to {}: Title='{}', Body='{}'", recipientEmail, title, body);
        return true;
    }
}
