package com.nqd.nqd_tool_content.config;

import com.nqd.nqd_tool_content.entity.User;
import com.nqd.nqd_tool_content.service.CustomOAuth2UserService;
import com.nqd.nqd_tool_content.service.impl.IdentityResolver;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.Optional;

@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final IdentityResolver identityResolver;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${server.servlet.session.cookie.domain:}")
    private String cookieDomain;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        CookieCsrfTokenRepository csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        if (cookieDomain != null && !cookieDomain.isBlank()) {
            csrfTokenRepository.setCookieCustomizer(customizer -> customizer.domain(cookieDomain));
        }

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(new org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler())
                        .ignoringRequestMatchers(
                                "/oauth2/**",
                                "/login/**",
                                "/media/**",
                                "/actuator/health",
                                "/api/v1/auth/logout",
                                "/api/v1/social/connections/**",
                                "/api/v1/ai/**",
                                "/api/v1/posts/*/chat",
                                "/api/v1/posts/*/regenerate*",
                                "/api/v1/posts/*/image*"
                        )
                )

                .addFilterAfter(new org.springframework.web.filter.OncePerRequestFilter() {
                    @Override
                    protected void doFilterInternal(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, jakarta.servlet.FilterChain filterChain)
                            throws jakarta.servlet.ServletException, java.io.IOException {
                        org.springframework.security.web.csrf.CsrfToken csrfToken = (org.springframework.security.web.csrf.CsrfToken) request.getAttribute(org.springframework.security.web.csrf.CsrfToken.class.getName());
                        if (csrfToken != null) {
                            csrfToken.getToken();
                        }
                        filterChain.doFilter(request, response);
                    }
                }, org.springframework.security.web.authentication.www.BasicAuthenticationFilter.class)

                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/",
                                "/error",
                                "/favicon.ico",
                                "/login/**",
                                "/oauth2/**",
                                "/media/**",
                                "/actuator/health",
                                "/api/v1/auth/me",
                                "/api/v1/auth/google-url",
                                "/api/v1/auth/facebook-url"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new org.springframework.security.web.authentication.HttpStatusEntryPoint(org.springframework.http.HttpStatus.UNAUTHORIZED))
                )
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                        .successHandler((request, response, authentication) -> {
                            String targetFrontend = frontendUrl != null ? frontendUrl.split(",")[0].trim() : "http://localhost:3000";
                            try {
                                OAuth2AuthenticationToken authToken = (OAuth2AuthenticationToken) authentication;
                                String provider = authToken.getAuthorizedClientRegistrationId();
                                OAuth2User oAuth2User = authToken.getPrincipal();

                                Optional<User> resolvedUserOpt = identityResolver
                                        .resolveAndAuthenticate(provider, oAuth2User, request);

                                if (resolvedUserOpt.isPresent()) {
                                    log.info("OAuth2 login SUCCESS for provider={}", provider);
                                    response.sendRedirect(targetFrontend + "/schedule");
                                } else {
                                    log.warn("OAuth2 login REJECTED (not in allowlist) for provider={}", provider);
                                    request.getSession().invalidate();
                                    response.sendRedirect(targetFrontend + "/login?error=not_allowed");
                                }
                            } catch (Exception ex) {
                                log.error("Unhandled error during OAuth2 success handling", ex);
                                response.sendRedirect(targetFrontend + "/login?error=auth_failed");
                            }
                        })
                        .failureHandler((request, response, exception) -> {
                            String targetFrontend = frontendUrl != null ? frontendUrl.split(",")[0].trim() : "http://localhost:3000";
                            log.error("OAuth2 authentication failure: {}", exception.getMessage(), exception);
                            response.sendRedirect(targetFrontend + "/login?error=auth_failed");
                        })
                )
                .logout(logout -> logout
                        .logoutUrl("/api/v1/auth/logout")
                        .logoutSuccessHandler((request, response, authentication) -> {
                            response.setStatus(HttpServletResponse.SC_OK);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"success\":true,\"message\":\"Logged out successfully\"}");
                        })
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        java.util.Set<String> origins = new java.util.LinkedHashSet<>();
        if (frontendUrl != null) {
            for (String u : frontendUrl.split(",")) {
                String trimmed = u.trim();
                if (!trimmed.isEmpty()) origins.add(trimmed);
            }
        }
        origins.add("http://localhost:3000");
        origins.add("https://nqdsm.site");
        origins.add("https://www.nqdsm.site");
        origins.add("https://api.nqdsm.site");
        configuration.setAllowedOrigins(new java.util.ArrayList<>(origins));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
