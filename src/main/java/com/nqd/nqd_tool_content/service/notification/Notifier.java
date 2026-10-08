package com.nqd.nqd_tool_content.service.notification;

public interface Notifier {

    String getChannelName(); // TELEGRAM, EMAIL, IN_APP

    boolean send(String recipient, String title, String body);
}
