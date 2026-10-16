package com.nqd.nqd_tool_content.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseSchemaMigrator implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        log.info("[DatabaseSchemaMigrator] Running schema auto-healing checks...");
        try {
            jdbcTemplate.execute("ALTER TABLE IF EXISTS post_templates ALTER COLUMN description TYPE TEXT");
            jdbcTemplate.execute("ALTER TABLE IF EXISTS post_templates ALTER COLUMN name TYPE TEXT");
            jdbcTemplate.execute("ALTER TABLE IF EXISTS post_templates ALTER COLUMN structure TYPE TEXT");
            jdbcTemplate.execute("ALTER TABLE IF EXISTS post_templates ALTER COLUMN example_content TYPE TEXT");
            jdbcTemplate.execute("UPDATE post_templates SET is_deleted = false WHERE is_deleted IS NULL");
            jdbcTemplate.execute("UPDATE post_templates SET is_default = false WHERE is_default IS NULL");
            jdbcTemplate.execute("UPDATE post_templates SET is_system = false WHERE is_system IS NULL");
            log.info("[DatabaseSchemaMigrator] post_templates schema verified and updated successfully");
        } catch (Exception e) {
            log.warn("[DatabaseSchemaMigrator] Migration notice (benign if non-postgres or already patched): {}", e.getMessage());
        }
    }
}
