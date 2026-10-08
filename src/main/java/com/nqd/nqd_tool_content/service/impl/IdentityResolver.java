package com.nqd.nqd_tool_content.service.impl;

import com.nqd.nqd_tool_content.entity.User;
import com.nqd.nqd_tool_content.entity.UserIdentity;
import com.nqd.nqd_tool_content.repository.UserIdentityRepository;
import com.nqd.nqd_tool_content.repository.UserRepository;
import com.nqd.nqd_tool_content.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdentityResolver {

    private final UserRepository userRepository;
    private final UserIdentityRepository userIdentityRepository;
    private final AuditService auditService;

    @Value("${auth.allowed-emails:}")
    private String allowedEmailsConfig;

    @Value("${auth.allowed-facebook-ids:}")
    private String allowedFacebookIdsConfig;

    /**
     * Resolves authenticated OAuth2User against allowlist.
     * If user is in allowlist, attaches identity to existing owner or creates single-user owner.
     * If not allowed, logs audit and returns Optional.empty().
     */
    @Transactional
    public Optional<User> resolveAndAuthenticate(String provider, OAuth2User oAuth2User, HttpServletRequest request) {
        String providerKey = provider.toUpperCase();
        String providerUserId;
        String email = null;
        String name = oAuth2User.getAttribute("name");
        String picture = null;
        String givenName = null;
        String familyName = null;

        if ("GOOGLE".equals(providerKey)) {
            providerUserId = oAuth2User.getAttribute("sub");
            email = oAuth2User.getAttribute("email");
            picture = oAuth2User.getAttribute("picture");
            givenName = oAuth2User.getAttribute("given_name");
            familyName = oAuth2User.getAttribute("family_name");
        } else if ("FACEBOOK".equals(providerKey)) {
            providerUserId = oAuth2User.getAttribute("id");
            email = oAuth2User.getAttribute("email"); // Can be null in Facebook
            picture = extractFacebookPicture(oAuth2User);
        } else {
            log.warn("Unsupported OAuth provider: {}", provider);
            return Optional.empty();
        }

        // 1. Check Allowlist
        if (!isAllowed(providerKey, email, providerUserId)) {
            log.warn("OAuth login rejected - not in allowlist. Provider={}, email={}, id={}", providerKey, email, providerUserId);
            auditService.logAction(null, "LOGIN_REJECTED", "USER", providerUserId,
                    String.format("Provider: %s, Email: %s, Reason: NOT_IN_ALLOWLIST", providerKey, email), request);
            return Optional.empty();
        }

        // 2. Lookup existing identity
        Optional<UserIdentity> existingIdentityOpt = userIdentityRepository
                .findByProviderAndProviderUserId(providerKey, providerUserId);

        final String finalEmail = email;
        final String finalName = name;
        final String finalGivenName = givenName;
        final String finalFamilyName = familyName;
        final String finalPicture = picture;

        User user;
        if (existingIdentityOpt.isPresent()) {
            UserIdentity identity = existingIdentityOpt.get();
            identity.setLastLoginAt(LocalDateTime.now());
            if (email != null && !email.isBlank()) {
                identity.setEmail(email);
            }
            userIdentityRepository.save(identity);

            user = userRepository.findById(identity.getUserId())
                    .orElseGet(() -> {
                        User newUser = createOrUpdateOwnerUser(finalEmail, finalName, finalGivenName, finalFamilyName, finalPicture, providerKey);
                        return userRepository.save(newUser);
                    });
        } else {
            // Check if owner user already exists in single-user mode
            user = getOrCreateSingleUser(email, name, givenName, familyName, picture, providerKey);
            if (user.getId() == null) {
                user = userRepository.save(user);
            }

            UserIdentity newIdentity = UserIdentity.builder()
                    .userId(user.getId())
                    .provider(providerKey)
                    .providerUserId(providerUserId)
                    .email(email)
                    .createdAt(LocalDateTime.now())
                    .lastLoginAt(LocalDateTime.now())
                    .build();
            userIdentityRepository.save(newIdentity);
            log.info("Linked new identity {} ({}) to user {}", providerKey, providerUserId, user.getId());
        }

        // Update user profile info
        if (name != null) user.setName(name);
        if (picture != null) user.setPictureUrl(picture);
        user.setAuthProvider(providerKey);
        user = userRepository.save(user);

        try {
            auditService.logAction(user.getId(), "LOGIN_SUCCESS", "USER", user.getId().toString(),
                    String.format("Provider: %s, Email: %s", providerKey, email), request);
        } catch (Exception ex) {
            log.warn("Could not write audit log for login: {}", ex.getMessage());
        }

        return Optional.of(user);
    }

    private boolean isAllowed(String provider, String email, String providerUserId) {
        Set<String> allowedEmails = parseSet(allowedEmailsConfig);
        Set<String> allowedFbIds = parseSet(allowedFacebookIdsConfig);

        if ("GOOGLE".equals(provider)) {
            // If allowed emails configured, check membership
            return email != null && (allowedEmails.isEmpty() || allowedEmails.contains(email.toLowerCase().trim()));
        } else if ("FACEBOOK".equals(provider)) {
            // Check either email or facebook id
            boolean emailOk = email != null && !allowedEmails.isEmpty() && allowedEmails.contains(email.toLowerCase().trim());
            boolean idOk = providerUserId != null && !allowedFbIds.isEmpty() && allowedFbIds.contains(providerUserId.trim());
            return allowedEmails.isEmpty() && allowedFbIds.isEmpty() || emailOk || idOk;
        }
        return false;
    }

    private Set<String> parseSet(String raw) {
        if (raw == null || raw.isBlank()) {
            return Collections.emptySet();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isEmpty())
                .collect(java.util.stream.Collectors.toSet());
    }

    private User getOrCreateSingleUser(String email, String name, String givenName, String familyName, String picture, String provider) {
        List<User> allUsers = userRepository.findAll();
        if (!allUsers.isEmpty()) {
            return allUsers.get(0); // Single-user owner
        }
        User newUser = createOrUpdateOwnerUser(email, name, givenName, familyName, picture, provider);
        return userRepository.save(newUser);
    }

    private User createOrUpdateOwnerUser(String email, String name, String givenName, String familyName, String picture, String provider) {
        return User.builder()
                .email(email)
                .name(name != null ? name : "Chủ sở hữu")
                .givenName(givenName)
                .familyName(familyName)
                .pictureUrl(picture)
                .role("ROLE_USER")
                .authProvider(provider)
                .build();
    }

    @SuppressWarnings("unchecked")
    private String extractFacebookPicture(OAuth2User oAuth2User) {
        Object pictureObj = oAuth2User.getAttribute("picture");
        if (pictureObj instanceof Map) {
            Map<String, Object> pictureMap = (Map<String, Object>) pictureObj;
            Object dataObj = pictureMap.get("data");
            if (dataObj instanceof Map) {
                Map<String, Object> dataMap = (Map<String, Object>) dataObj;
                Object url = dataMap.get("url");
                return url != null ? url.toString() : null;
            }
        }
        return null;
    }
}
