package com.nqd.nqd_tool_content.service;

import com.nqd.nqd_tool_content.dto.request.FakeConnectRequest;
import com.nqd.nqd_tool_content.dto.response.SocialAccountResponse;
import com.nqd.nqd_tool_content.dto.response.SocialConnectionResponse;

import java.util.List;
import java.util.UUID;

public interface SocialService {

    List<SocialConnectionResponse> getConnections(UUID userId);

    List<SocialAccountResponse> getAccounts(UUID userId);

    SocialAccountResponse toggleAccount(UUID userId, UUID accountId, boolean enabled);

    SocialConnectionResponse connectFake(UUID userId, FakeConnectRequest request);

    SocialConnectionResponse connectManual(UUID userId, com.nqd.nqd_tool_content.dto.request.ManualConnectRequest request);

    boolean healthCheck(UUID userId, UUID connectionId);

    void disconnect(UUID userId, UUID connectionId);
}

