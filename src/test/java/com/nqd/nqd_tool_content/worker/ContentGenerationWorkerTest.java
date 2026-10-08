package com.nqd.nqd_tool_content.worker;

import com.nqd.nqd_tool_content.entity.*;
import com.nqd.nqd_tool_content.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class ContentGenerationWorkerTest {

    @Autowired
    private ContentGenerationWorker contentGenerationWorker;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private ContentPlanRepository planRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SocialConnectionRepository connectionRepository;

    @Autowired
    private SocialAccountRepository accountRepository;

    @Autowired
    private PostMediaRepository postMediaRepository;

    private User testUser;
    private ContentPlan testPlan;
    private SocialAccount testAccount;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(User.builder()
                .email("ai_test_owner_" + UUID.randomUUID() + "@test.com")
                .name("AI Test Owner")
                .role("ROLE_USER")
                .build());

        SocialConnection conn = SocialConnection.builder()
                .userId(testUser.getId())
                .platform("FACEBOOK")
                .platformUserId("fb_u_1")
                .displayName("Facebook")
                .status("ACTIVE")
                .accessTokenEnc("v1:dummy")
                .build();
        conn = connectionRepository.save(conn);

        testAccount = SocialAccount.builder()
                .userId(testUser.getId())
                .connectionId(conn.getId())
                .platform("FACEBOOK")
                .accountType("PAGE")
                .platformAccountId("fb_page_1")
                .displayName("Fanpage")
                .isEnabled(true)
                .status("ACTIVE")
                .build();
        testAccount = accountRepository.save(testAccount);

        testPlan = ContentPlan.builder()
                .userId(testUser.getId())
                .name("Kế hoạch Test AI")
                .topic("Ứng dụng AI trong bán hàng trực tuyến")
                .instructions("Ngắn gọn, súc tích, mang tính chia sẻ kinh nghiệm")
                .language("vi")
                .tone("Chuyên nghiệp, truyền cảm hứng")
                .status("ACTIVE")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(2))
                .timezone("Asia/Ho_Chi_Minh")
                .postsPerDay(1)
                .includeImage(true)
                .imageStyle("minimalist")
                .build();
        testPlan = planRepository.save(testPlan);
    }

    @Test
    void testProcessPlannedPosts_GeneratesReadyPostsAndMedia() {
        UUID groupId = UUID.randomUUID();
        Post post = Post.builder()
                .userId(testUser.getId())
                .planId(testPlan.getId())
                .groupId(groupId)
                .slotKey("2026-10-07#0")
                .socialAccountId(testAccount.getId())
                .platform("FACEBOOK")
                .scheduledAt(LocalDateTime.now().plusHours(4))
                .generateAt(LocalDateTime.now().minusMinutes(5)) // Quá hạn generate_at -> phải sinh ngay
                .status("PLANNED")
                .angle("Mẹo thực chiến ứng dụng AI tiết kiệm 80% thời gian")
                .build();
        post = postRepository.save(post);

        // Chạy worker chu kỳ sinh nội dung
        contentGenerationWorker.runGenerationCycle();

        // Kiểm tra kết quả
        Post updated = postRepository.findById(post.getId()).orElseThrow();
        assertEquals("READY", updated.getStatus());
        assertNotNull(updated.getContent());
        assertFalse(updated.getContent().isBlank());
        assertNotNull(updated.getGeneratedAt());
        assertNotNull(updated.getAiProviderUsed());

        // Kiểm tra post_media đã gắn ảnh
        List<PostMedia> mediaList = postMediaRepository.findByPostIdOrderByOrderIndexAsc(post.getId());
        assertFalse(mediaList.isEmpty(), "Post phải được đính kèm ảnh");
        assertNotNull(mediaList.get(0).getMediaAssetId());
    }
}
