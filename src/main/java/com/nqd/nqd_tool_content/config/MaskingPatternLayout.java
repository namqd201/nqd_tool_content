package com.nqd.nqd_tool_content.config;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.nqd.nqd_tool_content.util.Redactor;

/**
 * Logback Appender / Filter hỗ trợ tự động che thông tin nhạy cảm qua Redactor (OBS-001).
 */
public class MaskingPatternLayout extends ch.qos.logback.classic.PatternLayout {

    @Override
    public String doLayout(ILoggingEvent event) {
        String message = super.doLayout(event);
        return Redactor.redact(message);
    }
}
