package com.nqd.nqd_tool_content.service;

import com.nqd.nqd_tool_content.dto.request.PostTemplateRequest;
import com.nqd.nqd_tool_content.dto.response.PostTemplateResponse;
import com.nqd.nqd_tool_content.entity.User;
import com.nqd.nqd_tool_content.repository.PostTemplateRepository;
import com.nqd.nqd_tool_content.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class PostTemplateServiceTest {

    @Autowired
    private PostTemplateService templateService;

    @Autowired
    private PostTemplateRepository templateRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(User.builder()
                .email("template_test_" + UUID.randomUUID() + "@test.com")
                .name("Template User")
                .role("ROLE_USER")
                .build());
    }

    @Test
    void testCreateTemplate_BasicAndLongDescription() {
        String longDesc = "Viết bài đăng Facebook giới thiệu website hoặc nền tảng trực tuyến, giúp người đọc hiểu giá trị của sản phẩm một cách tự nhiên, không gượng ép. ".repeat(5); // > 500 chars

        PostTemplateRequest req = PostTemplateRequest.builder()
                .name("Soft-Sell Website Promotion (Quảng cáo website tự nhiên)")
                .description(longDesc)
                .structure("""
[1. Hook]: Giới thiệu nhẹ nhàng
[2. Pain Point]: Vấn đề thường gặp
[3. Solution]: Giải pháp website cung cấp
[12. Nguyên tắc]: Không bịa đặt thông tin.
""")
                .exampleContent("Ví dụ bài viết mẫu...")
                .isDefault(false)
                .build();

        PostTemplateResponse res = templateService.createTemplate(testUser.getId(), req);
        assertNotNull(res);
        assertNotNull(res.getId());
        assertEquals("Soft-Sell Website Promotion (Quảng cáo website tự nhiên)", res.getName());
    }

    @Test
    void testCreateTemplate_WithDefaultFlag() {
        PostTemplateRequest req1 = PostTemplateRequest.builder()
                .name("Default Template 1")
                .structure("Step 1 -> Step 2")
                .isDefault(true)
                .build();
        PostTemplateResponse res1 = templateService.createTemplate(testUser.getId(), req1);
        assertTrue(res1.getIsDefault());

        PostTemplateRequest req2 = PostTemplateRequest.builder()
                .name("Default Template 2")
                .structure("Step A -> Step B")
                .isDefault(true)
                .build();
        PostTemplateResponse res2 = templateService.createTemplate(testUser.getId(), req2);
        assertTrue(res2.getIsDefault());

        List<PostTemplateResponse> list = templateService.listTemplates(testUser.getId());
        assertFalse(list.isEmpty());
    }
}
