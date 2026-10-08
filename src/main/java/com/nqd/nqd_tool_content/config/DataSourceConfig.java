package com.nqd.nqd_tool_content.config;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class DataSourceConfig {

    @Bean
    public BeanPostProcessor dataSourceSanitizer() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) {
                if (bean instanceof HikariDataSource ds) {
                    if (ds.getJdbcUrl() != null) {
                        String cleanUrl = clean(ds.getJdbcUrl());
                        ds.setJdbcUrl(cleanUrl);
                        log.info("Sanitized JDBC URL for HikariDataSource: {}", cleanUrl.replaceAll("password=[^&]*", "password=***"));
                    }
                    if (ds.getUsername() != null) {
                        ds.setUsername(clean(ds.getUsername()));
                    }
                    if (ds.getPassword() != null) {
                        ds.setPassword(clean(ds.getPassword()));
                    }
                }
                return bean;
            }

            private String clean(String str) {
                if (str == null) return null;
                String s = str.trim();
                if ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'"))) {
                    if (s.length() >= 2) {
                        s = s.substring(1, s.length() - 1).trim();
                    }
                }
                return s;
            }
        };
    }
}
