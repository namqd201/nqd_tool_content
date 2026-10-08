package com.nqd.nqd_tool_content.service;

import com.nqd.nqd_tool_content.dto.response.UserResponse;
import com.nqd.nqd_tool_content.entity.User;
import org.springframework.security.oauth2.core.user.OAuth2User;

public interface UserService {

    User processOAuthPostLogin(OAuth2User oAuth2User);

    UserResponse getUserByEmail(String email);
}
