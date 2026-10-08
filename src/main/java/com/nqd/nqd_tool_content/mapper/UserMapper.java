package com.nqd.nqd_tool_content.mapper;

import com.nqd.nqd_tool_content.dto.response.UserResponse;
import com.nqd.nqd_tool_content.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toUserResponse(User user) {
        if (user == null) {
            return null;
        }
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .givenName(user.getGivenName())
                .familyName(user.getFamilyName())
                .pictureUrl(user.getPictureUrl())
                .role(user.getRole())
                .authProvider(user.getAuthProvider())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
