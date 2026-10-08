package com.nqd.nqd_tool_content.service;

import com.nqd.nqd_tool_content.dto.request.CreatePlanRequest;
import com.nqd.nqd_tool_content.dto.response.PlanPreviewResponse;
import com.nqd.nqd_tool_content.dto.response.PlanResponse;
import com.nqd.nqd_tool_content.entity.SocialAccount;
import com.nqd.nqd_tool_content.entity.SocialConnection;
import com.nqd.nqd_tool_content.entity.User;
import com.nqd.nqd_tool_content.repository.PostRepository;
import com.nqd.nqd_tool_content.repository.SocialAccountRepository;
import com.nqd.nqd_tool_content.repository.SocialConnectionRepository;
import com.nqd.nqd_tool_content.repository.UserRepository;
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
public class PlanServiceTest {

    @Autowired
    private PlanService planService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SocialConnectionRepository connectionRepository;

    @Autowired
    private SocialAccountRepository accountRepository;

    @Autowired
    private PostRepository postRepository;

    private User testUser;
    private SocialAccount testAccount;

    @BeforeEach
    void setUp() {
        testUser = userRepository.findAll().stream().findFirst().orElseGet(() -> {
            User u = User.builder()
                    .email("plan_owner@test.com")
                    .name("Plan Owner")
                    .role("ROLE_USER")
                    .build();
            return userRepository.save(u);
        });

        SocialConnection conn = SocialConnection.builder()
                .userId(testUser.getId())
                .platform("FACEBOOK")
                .platformUserId("fb_user_123")
                .displayName("Facebook Connected")
                .status("ACTIVE")
                .accessTokenEnc("v1:dummy")
                .build();
        conn = connectionRepository.save(conn);

        testAccount = SocialAccount.builder()
                .userId(testUser.getId())
                .connectionId(conn.getId())
                .platform("FACEBOOK")
                .accountType("PAGE")
                .platformAccountId("fb_page_123")
                .displayName("My Coffee Shop Fanpage")
                .isEnabled(true)
                .status("ACTIVE")
                .build();
        testAccount = accountRepository.save(testAccount);
    }

    @Test
    void testPlanLifecycle_Preview_Draft_Activate_Cancel() {
        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = LocalDate.now().plusDays(3); // 3 ngày

        CreatePlanRequest req = CreatePlanRequest.builder()
                .name("Kế hoạch ra mắt cà phê mới")
                .topic("Tiếp thị cà phê thủ công cho giới trẻ")
                .instructions("Nhấn mạnh hương vị hoa quả tươi, ưu đãi 20%")
                .startDate(start)
                .endDate(end)
                .postsPerDay(2)
                .timeSlots(List.of("09:00", "15:00"))
                .targetAccountIds(List.of(testAccount.getId()))
                .includeImage(true)
                .build();

        // 1. Preview
        PlanPreviewResponse preview = planService.preview(req);
        assertEquals(3, preview.getTotalDays());
        assertEquals(2, preview.getPostsPerDay());
        assertEquals(6, preview.getTotalSlotsPerChannel());
        assertEquals(6, preview.getTotalPosts());
        assertTrue(preview.getEstimatedCostUsd().doubleValue() > 0);

        // 2. Tạo bản nháp (DRAFT)
        PlanResponse draft = planService.createDraft(testUser.getId(), req);
        assertNotNull(draft.getId());
        assertEquals("DRAFT", draft.getStatus());
        assertEquals(0, draft.getTotalSlots());

        // 3. Kích hoạt (ACTIVE)
        PlanResponse active = planService.activatePlan(testUser.getId(), UUID.fromString(draft.getId()));
        assertEquals("ACTIVE", active.getStatus());
        assertEquals(6, active.getTotalSlots());

        // Kiểm tra posts đã được sinh
        long postCount = postRepository.countByPlanIdAndIsDeletedFalse(UUID.fromString(draft.getId()));
        assertEquals(6, postCount);

        // 4. Hủy (CANCEL)
        planService.cancelPlan(testUser.getId(), UUID.fromString(draft.getId()));
        PlanResponse cancelled = planService.getPlan(testUser.getId(), UUID.fromString(draft.getId()));
        assertEquals("CANCELLED", cancelled.getStatus());

        // Các post chưa chạy phải chuyển sang SKIPPED
        long skippedCount = postRepository.countByPlanIdAndStatusAndIsDeletedFalse(UUID.fromString(draft.getId()), "SKIPPED");
        assertEquals(6, skippedCount);
    }
}
