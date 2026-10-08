package com.nqd.nqd_tool_content.controller;

import com.nqd.nqd_tool_content.dto.response.AuthResponse;
import com.nqd.nqd_tool_content.dto.response.UserResponse;
import com.nqd.nqd_tool_content.entity.User;
import com.nqd.nqd_tool_content.entity.UserIdentity;
import com.nqd.nqd_tool_content.mapper.UserMapper;
import com.nqd.nqd_tool_content.repository.UserIdentityRepository;
import com.nqd.nqd_tool_content.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final UserIdentityRepository userIdentityRepository;
    private final UserMapper userMapper;

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    AuthResponse.builder()
                            .authenticated(false)
                            .message("Chưa đăng nhập")
                            .build()
            );
        }

        User user = null;
        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            String provider = oauthToken.getAuthorizedClientRegistrationId().toUpperCase();
            OAuth2User principal = oauthToken.getPrincipal();
            String providerUserId = "GOOGLE".equals(provider) ? principal.getAttribute("sub") : principal.getAttribute("id");

            if (providerUserId != null) {
                Optional<UserIdentity> identityOpt = userIdentityRepository
                        .findByProviderAndProviderUserId(provider, providerUserId);
                if (identityOpt.isPresent()) {
                    user = userRepository.findById(identityOpt.get().getUserId()).orElse(null);
                }
            }
        }

        if (user == null) {
            List<User> users = userRepository.findAll();
            if (!users.isEmpty()) {
                user = users.get(0);
            }
        }

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    AuthResponse.builder()
                            .authenticated(false)
                            .message("Không tìm thấy người dùng")
                            .build()
            );
        }

        UserResponse userResponse = userMapper.toUserResponse(user);
        return ResponseEntity.ok(
                AuthResponse.builder()
                        .authenticated(true)
                        .message("Xác thực thành công")
                        .user(userResponse)
                        .build()
        );
    }

    @GetMapping("/identities")
    public ResponseEntity<List<UserIdentity>> getIdentities(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<User> users = userRepository.findAll();
        if (users.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(userIdentityRepository.findByUserId(users.get(0).getId()));
    }

    @GetMapping("/google-url")
    public ResponseEntity<Map<String, String>> getGoogleAuthUrl() {
        return ResponseEntity.ok(Map.of(
                "authorizationUrl", "/oauth2/authorization/google"
        ));
    }

    @GetMapping("/facebook-url")
    public ResponseEntity<Map<String, String>> getFacebookAuthUrl() {
        return ResponseEntity.ok(Map.of(
                "authorizationUrl", "/oauth2/authorization/facebook"
        ));
    }
}
