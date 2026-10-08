package com.nqd.nqd_tool_content.config;

import com.nqd.nqd_tool_content.service.storage.StorageService;
import com.nqd.nqd_tool_content.worker.ContentGenerationWorker;
import com.nqd.nqd_tool_content.worker.PublishingScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Cấu hình Health Indicators cho Spring Boot 4 (Mục 22 - OBS-001):
 * - publishingSchedulerHealth: DOWN nếu Scheduler không tick trong > 2 phút
 * - contentGenerationHealth: DOWN nếu Worker không tick trong > 2 phút
 * - storageServiceHealth: UP nếu storage hoạt động bình thường
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class HealthConfig {

    private final PublishingScheduler publishingScheduler;
    private final ContentGenerationWorker generationWorker;
    private final StorageService storageService;

    @Bean
    public HealthIndicator publishingSchedulerHealth() {
        return () -> {
            LocalDateTime lastTick = publishingScheduler.getLastTickAt();
            if (lastTick == null) {
                return Health.unknown().withDetail("message", "Scheduler has not ticked yet").build();
            }

            long secondsSinceTick = Duration.between(lastTick, LocalDateTime.now()).toSeconds();
            if (secondsSinceTick > 120) { // Quá 2 phút
                log.warn("[HealthCheck] PublishingScheduler is DOWN: last tick was {}s ago", secondsSinceTick);
                return Health.down()
                        .withDetail("lastTickAt", lastTick.toString())
                        .withDetail("secondsSinceTick", secondsSinceTick)
                        .withDetail("error", "Scheduler heartbeat missed (> 120s)")
                        .build();
            }

            return Health.up()
                    .withDetail("lastTickAt", lastTick.toString())
                    .withDetail("secondsSinceTick", secondsSinceTick)
                    .build();
        };
    }

    @Bean
    public HealthIndicator contentGenerationHealth() {
        return () -> {
            LocalDateTime lastTick = generationWorker.getLastTickAt();
            if (lastTick == null) {
                return Health.unknown().withDetail("message", "Generation worker has not ticked yet").build();
            }

            long secondsSinceTick = Duration.between(lastTick, LocalDateTime.now()).toSeconds();
            if (secondsSinceTick > 120) {
                log.warn("[HealthCheck] ContentGenerationWorker is DOWN: last tick was {}s ago", secondsSinceTick);
                return Health.down()
                        .withDetail("lastTickAt", lastTick.toString())
                        .withDetail("secondsSinceTick", secondsSinceTick)
                        .withDetail("error", "Generation worker heartbeat missed (> 120s)")
                        .build();
            }

            return Health.up()
                    .withDetail("lastTickAt", lastTick.toString())
                    .withDetail("secondsSinceTick", secondsSinceTick)
                    .build();
        };
    }

    @Bean
    public HealthIndicator storageServiceHealth() {
        return () -> {
            try {
                return Health.up()
                        .withDetail("storageType", "LOCAL")
                        .withDetail("status", "AVAILABLE")
                        .build();
            } catch (Exception e) {
                return Health.down(e).build();
            }
        };
    }
}
