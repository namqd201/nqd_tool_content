package com.nqd.nqd_tool_content.util;

import java.util.regex.Pattern;

public final class Redactor {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("(?i)(bearer\\s+|token[\"':\\s=]+|secret[\"':\\s=]+|password[\"':\\s=]+)([A-Za-z0-9_\\-\\.~+/=]{8,})");

    private Redactor() {}

    public static String redact(String input) {
        if (input == null) {
            return null;
        }
        return TOKEN_PATTERN.matcher(input).replaceAll("$1[REDACTED]");
    }
}
