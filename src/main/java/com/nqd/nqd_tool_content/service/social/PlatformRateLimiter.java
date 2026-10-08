package com.nqd.nqd_tool_content.service.social;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Token bucket rate limiter cho từng kênh mạng xã hội và nền tảng (ADAPT-006).
 * Đảm bảo hệ thống không vi phạm rate limit của từng nền tảng khi xuất bản tự động.
 */
@Slf4j
@Component
public class PlatformRateLimiter {

    // Lưu trữ số token khả dụng và thời điểm nạp gần nhất cho từng account
    private final Map<UUID, BucketState> accountBuckets = new ConcurrentHashMap<>();

    // Cấu hình mặc định:
    // Facebook Page: 50 requests / 10 phút (~5 requests/phút)
    // Threads: 30 requests / 10 phút (~3 requests/phút)
    // X (Twitter): 15 requests / 15 phút (~1 request/phút gói Free/Basic)
    // LinkedIn: 20 requests / 10 phút (~2 requests/phút)
    private static class BucketState {
        int tokens;
        final int maxTokens;
        final long refillIntervalMs;
        final int refillAmount;
        long lastRefillTime;

        BucketState(int maxTokens, long refillIntervalMs, int refillAmount) {
            this.tokens = maxTokens;
            this.maxTokens = maxTokens;
            this.refillIntervalMs = refillIntervalMs;
            this.refillAmount = refillAmount;
            this.lastRefillTime = System.currentTimeMillis();
        }

        synchronized boolean tryConsume() {
            refill();
            if (tokens > 0) {
                tokens--;
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.currentTimeMillis();
            long elapsed = now - lastRefillTime;
            if (elapsed >= refillIntervalMs) {
                int refills = (int) (elapsed / refillIntervalMs);
                tokens = Math.min(maxTokens, tokens + refills * refillAmount);
                lastRefillTime = now;
            }
        }
    }

    /**
     * Thử lấy token để thực hiện request đăng bài cho accountId.
     * @return true nếu còn hạn mức, false nếu bị vượt hạn mức (cần lùi lịch)
     */
    public boolean tryAcquire(UUID accountId, String platform) {
        BucketState bucket = accountBuckets.computeIfAbsent(accountId, id -> createBucketForPlatform(platform));
        boolean acquired = bucket.tryConsume();
        if (!acquired) {
            log.warn("[PlatformRateLimiter] Rate limit exceeded for account {} on platform {}", accountId, platform);
        }
        return acquired;
    }

    private BucketState createBucketForPlatform(String platform) {
        if (platform == null) {
            return new BucketState(20, 60_000, 5);
        }
        return switch (platform.toUpperCase()) {
            case "FACEBOOK" -> new BucketState(25, 60_000, 5); // 25 tokens, hồi 5 mỗi phút
            case "THREADS" -> new BucketState(15, 60_000, 3);
            case "X" -> new BucketState(10, 60_000, 2);
            case "LINKEDIN" -> new BucketState(15, 60_000, 3);
            default -> new BucketState(20, 60_000, 5);
        };
    }
}
