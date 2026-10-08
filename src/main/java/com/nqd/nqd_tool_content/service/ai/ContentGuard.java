package com.nqd.nqd_tool_content.service.ai;

import com.nqd.nqd_tool_content.service.ai.dto.GuardResult;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

@Component
public class ContentGuard {

    private static final Map<String, Integer> PLATFORM_CHAR_LIMITS = Map.of(
            "X", 280,
            "THREADS", 500,
            "FACEBOOK", 5000,
            "LINKEDIN", 3000
    );

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("(\\{\\{.*?\\}\\}|\\[.*?\\]|TODO|PLACEHOLDER)");

    public GuardResult checkContent(String platform, String content, List<String> forbiddenWords, List<String> recentPosts) {
        GuardResult result = GuardResult.pass();

        if (content == null || content.trim().isEmpty()) {
            return GuardResult.reject("Nội dung bài viết không được để trống.");
        }

        String trimmed = content.trim();

        // 1. Kiểm tra giới hạn ký tự
        Integer limit = PLATFORM_CHAR_LIMITS.get(platform.toUpperCase());
        if (limit != null && trimmed.length() > limit) {
            return GuardResult.reject("Nội dung vượt quá giới hạn " + limit + " ký tự của " + platform + " (hiện có " + trimmed.length() + " ký tự).");
        }

        // 2. Kiểm tra placeholder sót lại
        if (PLACEHOLDER_PATTERN.matcher(trimmed).find()) {
            return GuardResult.reject("Nội dung có chứa placeholder chưa được thay thế (dạng {{...}}, [...] hoặc TODO).");
        }

        // 3. Kiểm tra từ cấm
        if (forbiddenWords != null) {
            for (String fw : forbiddenWords) {
                if (fw != null && !fw.isBlank() && trimmed.toLowerCase().contains(fw.trim().toLowerCase())) {
                    return GuardResult.reject("Nội dung chứa từ cấm trong quy chuẩn: '" + fw.trim() + "'.");
                }
            }
        }

        // 4. Kiểm tra chống lặp với các bài gần nhất
        if (recentPosts != null) {
            for (String past : recentPosts) {
                if (past != null && !past.isBlank()) {
                    double similarity = calculateJaccardSimilarity(trimmed, past.trim());
                    if (similarity >= 0.8) {
                        return GuardResult.reject("Nội dung quá trùng lặp với bài viết đã đăng gần đây (độ tương đồng " + Math.round(similarity * 100) + "%).");
                    } else if (similarity >= 0.6) {
                        result = GuardResult.warning("Nội dung có phần tương tự với bài đăng trước đó (độ tương đồng " + Math.round(similarity * 100) + "%).");
                    }
                }
            }
        }

        return result;
    }

    private double calculateJaccardSimilarity(String s1, String s2) {
        Set<String> set1 = new HashSet<>(Arrays.asList(s1.toLowerCase().split("\\s+")));
        Set<String> set2 = new HashSet<>(Arrays.asList(s2.toLowerCase().split("\\s+")));

        if (set1.isEmpty() && set2.isEmpty()) return 1.0;
        if (set1.isEmpty() || set2.isEmpty()) return 0.0;

        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        return (double) intersection.size() / union.size();
    }
}
