package com.nqd.nqd_tool_content.worker;

import com.nqd.nqd_tool_content.entity.*;
import com.nqd.nqd_tool_content.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class PublishingPipelineTest {

    @Autowired
    private PublishingScheduler publishingScheduler;

    @Autowired
    private PublishWorker publishWorker;

    @Autowired
    private Reaper reaper;

    @Autowired
    private Reconciler reconciler;

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
    private PostAttemptRepository postAttemptRepository;

    private User testUser;
    private SocialAccount testAccount;

    @Autowired
    private com.nqd.nqd_tool_content.util.CryptoUtil cryptoUtil;

    @BeforeEach
    void setUp() {
        testUser = userRepository.findAll().stream().findFirst().orElseGet(() -> {
            User u = User.builder()
                    .email("pub_tester@test.com")
                    .name("Publisher Tester")
                    .role("ROLE_USER")
                    .build();
            return userRepository.save(u);
        });

        UUID tempConnId = UUID.randomUUID();
        String encToken = cryptoUtil.encrypt("valid_test_token", tempConnId + ":access_token");

        SocialConnection conn = SocialConnection.builder()
                .userId(testUser.getId())
                .platform("FACEBOOK")
                .platformUserId("fb_pub_user_1")
                .displayName("Facebook Main")
                .status("ACTIVE")
                .accessTokenEnc(encToken)
                .build();
        conn = connectionRepository.save(conn);

        // Re-encrypt with actual saved connection id
        conn.setAccessTokenEnc(cryptoUtil.encrypt("valid_test_token", conn.getId() + ":access_token"));
        conn = connectionRepository.save(conn);

        testAccount = SocialAccount.builder()
                .userId(testUser.getId())
                .connectionId(conn.getId())
                .platform("FACEBOOK")
                .accountType("PAGE")
                .platformAccountId("page_123")
                .displayName("Fanpage Pub")
                .isEnabled(true)
                .status("ACTIVE")
                .build();
        testAccount = accountRepository.save(testAccount);
    }

    @Test
    void testAtomicClaimAndSuccessfulPublish() {
        // Tạo bài READY đến hạn xuất bản
        Post post = Post.builder()
                .userId(testUser.getId())
                .slotKey("2026-10-07#0")
                .socialAccountId(testAccount.getId())
                .platform("FACEBOOK")
                .scheduledAt(LocalDateTime.now().minusMinutes(1))
                .nextAttemptAt(LocalDateTime.now().minusMinutes(1))
                .status("READY")
                .content("Nội dung sẵn sàng đăng lên mạng xã hội #test")
                .build();
        post = postRepository.save(post);

        // 1. Claim bằng scheduler
        List<UUID> claimed = publishingScheduler.claimReadyPosts();
        assertTrue(claimed.contains(post.getId()), "Bài viết READY phải được claim thành công");

        Post claimedPost = postRepository.findById(post.getId()).orElseThrow();
        assertEquals("PUBLISHING", claimedPost.getStatus());
        assertNotNull(claimedPost.getLockedBy());
        assertNotNull(claimedPost.getLeaseExpiresAt());

        // 2. PublishWorker thực thi xuất bản
        publishWorker.processPost(post.getId(), "test-worker-1");

        Post published = postRepository.findById(post.getId()).orElseThrow();
        assertEquals("PUBLISHED", published.getStatus());
        assertNotNull(published.getPlatformPostId());
        assertNotNull(published.getPlatformPostUrl());
        assertNotNull(published.getPublishedAt());

        // Kiểm tra post_attempts
        List<PostAttempt> attempts = postAttemptRepository.findByPostIdOrderByAttemptNoAsc(post.getId());
        assertFalse(attempts.isEmpty());
        assertEquals("SUCCESS", attempts.get(0).getOutcome());
    }

    @Test
    void testReaperReclaimsExpiredLease() {
        // Tạo bài PUBLISHING đã hết hạn lease nhưng request_sent_at là NULL (chưa gọi mạng)
        Post post = Post.builder()
                .userId(testUser.getId())
                .slotKey("2026-10-07#1")
                .socialAccountId(testAccount.getId())
                .platform("FACEBOOK")
                .scheduledAt(LocalDateTime.now().minusMinutes(10))
                .status("PUBLISHING")
                .lockedBy("crashed-worker")
                .leaseExpiresAt(LocalDateTime.now().minusMinutes(2)) // Expired
                .requestSentAt(null)
                .attemptCount(1)
                .content("Bài viết bị crash")
                .build();
        post = postRepository.save(post);

        // Reaper chạy
        reaper.reapExpiredLeases();

        Post recovered = postRepository.findById(post.getId()).orElseThrow();
        assertEquals("READY", recovered.getStatus(), "Bài chưa gửi request khi hết lease phải được hoàn về READY");
        assertNull(recovered.getLockedBy());
        assertEquals(0, recovered.getAttemptCount()); // Hoàn lại attempt count
    }

    @Test
    void testReconcilerResolvesUncertainPost() {
        // Tạo bài RECONCILING
        Post post = Post.builder()
                .userId(testUser.getId())
                .slotKey("2026-10-07#2")
                .socialAccountId(testAccount.getId())
                .platform("FACEBOOK")
                .scheduledAt(LocalDateTime.now().minusMinutes(5))
                .status("RECONCILING")
                .nextAttemptAt(LocalDateTime.now().minusMinutes(1))
                .reconcileCount(0)
                .attemptCount(1)
                .maxAttempts(3)
                .content("Bài viết timeout mạng")
                .build();
        post = postRepository.save(post);

        // Lần kiểm tra 1: Hẹn sau 2 phút
        reconciler.reconcilePost(post);
        assertEquals("RECONCILING", post.getStatus());
        assertEquals(1, post.getReconcileCount());

        // Lần kiểm tra 2: Chắc chắn chưa đăng -> Hoàn về READY để retry an toàn
        reconciler.reconcilePost(post);
        assertEquals("READY", post.getStatus());
        assertEquals(2, post.getReconcileCount());
    }
}
