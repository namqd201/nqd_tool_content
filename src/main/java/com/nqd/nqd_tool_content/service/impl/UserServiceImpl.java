package com.nqd.nqd_tool_content.service.impl;

import com.nqd.nqd_tool_content.dto.response.UserResponse;
import com.nqd.nqd_tool_content.entity.User;
import com.nqd.nqd_tool_content.mapper.UserMapper;
import com.nqd.nqd_tool_content.repository.UserRepository;
import com.nqd.nqd_tool_content.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public User processOAuthPostLogin(OAuth2User oAuth2User) {
        String googleId = oAuth2User.getAttribute("sub");
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String givenName = oAuth2User.getAttribute("given_name");
        String familyName = oAuth2User.getAttribute("family_name");
        String picture = oAuth2User.getAttribute("picture");

        log.info("Processing Google OAuth login for email: {}", email);

        Optional<User> existUserOpt = userRepository.findByEmail(email);

        User user;
        if (existUserOpt.isPresent()) {
            user = existUserOpt.get();
            user.setName(name);
            user.setGivenName(givenName);
            user.setFamilyName(familyName);
            user.setPictureUrl(picture);
            user.setGoogleId(googleId);
            log.info("Updated existing user profile: {}", email);
        } else {
            user = User.builder()
                    .email(email)
                    .name(name)
                    .givenName(givenName)
                    .familyName(familyName)
                    .pictureUrl(picture)
                    .googleId(googleId)
                    .role("ROLE_USER")
                    .authProvider("GOOGLE")
                    .build();
            log.info("Registered new user from Google OAuth: {}", email);
        }

        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(userMapper::toUserResponse)
                .orElse(null);
    }
}
