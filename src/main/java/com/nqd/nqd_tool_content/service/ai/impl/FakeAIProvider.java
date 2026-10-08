package com.nqd.nqd_tool_content.service.ai.impl;

import com.nqd.nqd_tool_content.service.ai.AIProvider;
import com.nqd.nqd_tool_content.service.ai.dto.AIContentRequest;
import com.nqd.nqd_tool_content.service.ai.dto.AIContentResponse;
import com.nqd.nqd_tool_content.service.ai.dto.GeneratedPlatformPost;
import com.nqd.nqd_tool_content.service.ai.dto.PlanAnglesRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class FakeAIProvider implements AIProvider {

    @Override
    public String getProviderName() {
        return "FAKE";
    }

    @Override
    public AIContentResponse generateContent(AIContentRequest request) {
        log.info("[FakeAIProvider] Generating mock content for topic: {}, platforms: {}", request.getTopic(), request.getPlatforms());
        List<GeneratedPlatformPost> posts = new ArrayList<>();

        for (String platform : request.getPlatforms()) {
            String p = platform.toUpperCase();
            String content;
            List<String> hashtags = new ArrayList<>(request.getPreferredHashtags());
            if (hashtags.isEmpty()) {
                hashtags = List.of("#NQDSMTool", "#AutoPost", "#SocialMedia");
            }

            if ("X".equals(p)) {
                content = "Chia sẻ nhanh về " + request.getTopic() + ": Luôn tối ưu quy trình để đạt hiệu quả cao nhất mỗi ngày! " + String.join(" ", hashtags);
            } else if ("THREADS".equals(p)) {
                content = "Góc suy ngẫm về " + request.getTopic() + ".\n\nBạn có đang gặp khó khăn khi xây dựng nội dung đều đặn? Hãy để công nghệ hỗ trợ bạn.";
            } else if ("LINKEDIN".equals(p)) {
                content = "Tối ưu hóa chiến lược nội dung: Góc nhìn về " + request.getTopic() + ".\n\n" +
                        "Trong thời đại số hóa, sự nhất quán là chìa khóa mở ra sự tin cậy. Dưới đây là 3 điểm cốt lõi bạn nên lưu ý:\n" +
                        "1. Định vị rõ chân dung khách hàng.\n" +
                        "2. Truyền tải thông điệp ngắn gọn, trực diện.\n" +
                        "3. Tận dụng tự động hóa thông minh.\n\n" +
                        "Bạn nghĩ sao về xu hướng này?";
            } else {
                content = "Chào cả nhà! Hôm nay chúng ta cùng khám phá về chủ đề " + request.getTopic() + " nhé.\n\n" +
                        "Một mẹo nhỏ nhưng cực kỳ hữu ích giúp bạn tiết kiệm 50% thời gian mỗi tuần: Hãy lên kế hoạch nội dung từ trước!\n\n" +
                        "👉 Đừng quên để lại bình luận chia sẻ kinh nghiệm của bạn nhé!";
            }

            posts.add(GeneratedPlatformPost.builder()
                    .platform(p)
                    .content(content)
                    .hashtags(hashtags)
                    .build());
        }

        return AIContentResponse.builder()
                .posts(posts)
                .suggestedImagePrompt("A modern minimalist workspace illustration showcasing creativity and efficiency")
                .promptTokens(150)
                .completionTokens(280)
                .estimatedCostUsd(new BigDecimal("0.0050"))
                .modelUsed("mock-model-v1")
                .providerUsed(getProviderName())
                .build();
    }

    @Override
    public List<String> generatePlanAngles(PlanAnglesRequest request) {
        log.info("[FakeAIProvider] Generating mock angles for topic: {}", request.getTopic());
        List<String> angles = new ArrayList<>();
        int count = Math.max(1, request.getCount());
        for (int i = 1; i <= count; i++) {
            angles.add("Góc nhìn " + i + ": Khía cạnh thực chiến và mẹo ứng dụng của " + request.getTopic());
        }
        return angles;
    }
}
