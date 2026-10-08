package com.nqd.nqd_tool_content.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "name")
    private String name;

    @Column(name = "given_name")
    private String givenName;

    @Column(name = "family_name")
    private String familyName;

    @Column(name = "picture_url", length = 1000)
    private String pictureUrl;

    @Column(name = "google_id", unique = true)
    private String googleId;

    @Builder.Default
    @Column(name = "role")
    private String role = "ROLE_USER";

    @Builder.Default
    @Column(name = "auth_provider")
    private String authProvider = "GOOGLE";
}
