package com.nqd.nqd_tool_content.service;

import com.nqd.nqd_tool_content.dto.request.PostTemplateRequest;
import com.nqd.nqd_tool_content.dto.response.PostTemplateResponse;
import com.nqd.nqd_tool_content.entity.PostTemplate;
import com.nqd.nqd_tool_content.exception.BusinessException;
import com.nqd.nqd_tool_content.repository.PostTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostTemplateService {

    private final PostTemplateRepository templateRepository;

    @Transactional
    public List<PostTemplateResponse> listTemplates(UUID userId) {
        List<PostTemplate> list = templateRepository.findAllAvailableForUser(userId);
        if (list.isEmpty()) {
            seedDefaultTemplates(userId);
            list = templateRepository.findAllAvailableForUser(userId);
        }
        return list.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional
    public PostTemplateResponse getTemplate(UUID userId, UUID id) {
        PostTemplate template = templateRepository.findById(id)
                .filter(t -> (t.getUserId().equals(userId) || Boolean.TRUE.equals(t.getIsSystem())) && !t.getIsDeleted())
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy form mẫu bài viết"));
        return mapToResponse(template);
    }

    @Transactional
    public PostTemplateResponse createTemplate(UUID userId, PostTemplateRequest req) {
        if (Boolean.TRUE.equals(req.getIsDefault())) {
            unsetExistingDefault(userId);
        }

        PostTemplate template = PostTemplate.builder()
                .userId(userId)
                .name(req.getName().trim())
                .description(req.getDescription() != null ? req.getDescription().trim() : null)
                .structure(req.getStructure().trim())
                .exampleContent(req.getExampleContent() != null ? req.getExampleContent().trim() : null)
                .isDefault(Boolean.TRUE.equals(req.getIsDefault()))
                .isSystem(false)
                .build();

        return mapToResponse(templateRepository.save(template));
    }

    @Transactional
    public PostTemplateResponse updateTemplate(UUID userId, UUID id, PostTemplateRequest req) {
        PostTemplate template = templateRepository.findById(id)
                .filter(t -> !t.getIsDeleted())
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy form mẫu bài viết"));

        // Nếu là system template, khi user sửa ta sẽ tạo bản sao cho user đó
        if (Boolean.TRUE.equals(template.getIsSystem()) && !template.getUserId().equals(userId)) {
            PostTemplate userCopy = PostTemplate.builder()
                    .userId(userId)
                    .name(req.getName() != null ? req.getName().trim() : template.getName())
                    .description(req.getDescription() != null ? req.getDescription().trim() : template.getDescription())
                    .structure(req.getStructure() != null ? req.getStructure().trim() : template.getStructure())
                    .exampleContent(req.getExampleContent() != null ? req.getExampleContent().trim() : template.getExampleContent())
                    .isDefault(Boolean.TRUE.equals(req.getIsDefault()))
                    .isSystem(false)
                    .build();
            if (Boolean.TRUE.equals(req.getIsDefault())) {
                unsetExistingDefault(userId);
            }
            return mapToResponse(templateRepository.save(userCopy));
        }

        if (Boolean.TRUE.equals(req.getIsDefault()) && !Boolean.TRUE.equals(template.getIsDefault())) {
            unsetExistingDefault(userId);
        }

        if (req.getName() != null) template.setName(req.getName().trim());
        if (req.getDescription() != null) template.setDescription(req.getDescription().trim());
        if (req.getStructure() != null) template.setStructure(req.getStructure().trim());
        if (req.getExampleContent() != null) template.setExampleContent(req.getExampleContent().trim());
        if (req.getIsDefault() != null) template.setIsDefault(req.getIsDefault());

        return mapToResponse(templateRepository.save(template));
    }

    @Transactional
    public void deleteTemplate(UUID userId, UUID id) {
        PostTemplate template = templateRepository.findById(id)
                .filter(t -> t.getUserId().equals(userId) && !t.getIsDeleted())
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy form mẫu để xóa hoặc bạn không có quyền"));

        if (Boolean.TRUE.equals(template.getIsSystem())) {
            throw new BusinessException("PERMISSION_DENIED", "Không thể xóa form mẫu mặc định của hệ thống");
        }

        template.setIsDeleted(true);
        templateRepository.save(template);
    }

    @Transactional
    public PostTemplateResponse setDefault(UUID userId, UUID id) {
        unsetExistingDefault(userId);
        PostTemplate template = templateRepository.findById(id)
                .filter(t -> (t.getUserId().equals(userId) || Boolean.TRUE.equals(t.getIsSystem())) && !t.getIsDeleted())
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy form mẫu"));

        if (Boolean.TRUE.equals(template.getIsSystem()) && !template.getUserId().equals(userId)) {
            // Tạo bản copy riêng của user và đặt default
            PostTemplate userCopy = PostTemplate.builder()
                    .userId(userId)
                    .name(template.getName())
                    .description(template.getDescription())
                    .structure(template.getStructure())
                    .exampleContent(template.getExampleContent())
                    .isDefault(true)
                    .isSystem(false)
                    .build();
            return mapToResponse(templateRepository.save(userCopy));
        }

        template.setIsDefault(true);
        return mapToResponse(templateRepository.save(template));
    }

    private void unsetExistingDefault(UUID userId) {
        templateRepository.findByUserIdAndIsDefaultTrueAndIsDeletedFalse(userId)
                .ifPresent(t -> {
                    t.setIsDefault(false);
                    templateRepository.save(t);
                });
    }

    private void seedDefaultTemplates(UUID userId) {
        log.info("[PostTemplateService] Seeding initial standard post templates for user {}", userId);

        List<PostTemplate> defaults = List.of(
                PostTemplate.builder()
                        .userId(userId)
                        .name("AIDA (Thu hút - Quan tâm - Khao khát - Hành động)")
                        .description("Công thức kinh điển chuyển đổi cao, tạo ấn tượng mạnh từ câu đầu tiên và thúc đẩy hành động.")
                        .structure("""
[1. Hook / Tiêu đề]: 1 câu giật tít bắt mắt, đánh thẳng vào tâm lý tò mò hoặc sự thật bất ngờ.
[2. Quan tâm (Interest)]: Nêu câu chuyện ngắn hoặc thực trạng khiến người đọc thấy mình trong đó.
[3. Khao khát (Desire)]: Nêu bật giá trị cốt lõi, bí quyết hoặc giải pháp đột phá giúp giải quyết vấn đề.
[4. Kêu gọi hành động (CTA)]: Hướng dẫn cụ thể người đọc cần làm gì (bình luận, nhắn tin, lưu bài viết).
[5. Hashtags]: 3-5 hashtag liên quan trực tiếp đến chủ đề.""")
                        .isDefault(true)
                        .isSystem(true)
                        .build(),

                PostTemplate.builder()
                        .userId(userId)
                        .name("PAS (Vấn đề - Đào sâu nỗi đau - Giải pháp)")
                        .description("Tập trung vào nỗi đau và vấn đề nhức nhối của khách hàng, sau đó đưa ra giải pháp triệt để.")
                        .structure("""
[1. Vấn đề (Problem)]: Nêu đúng khó khăn, vướng mắc nhức nhối mà độc giả đang gặp phải mỗi ngày.
[2. Đào sâu (Agitate)]: Phân tích hệ quả, sự lãng phí thời gian/tiền bạc và cảm giác bế tắc nếu không sửa ngay.
[3. Giải pháp (Solve)]: Đưa ra cách xử lý dứt điểm, rõ ràng từng bước dễ hiểu.
[4. Lời khuyên & CTA]: Nhắc nhở kiên trì và kêu gọi lưu lại bài viết để áp dụng.
[5. Hashtags]: Hashtags chuyên môn và giải pháp.""")
                        .isDefault(false)
                        .isSystem(true)
                        .build(),

                PostTemplate.builder()
                        .userId(userId)
                        .name("Storytelling (Kể chuyện trải nghiệm - Đúc kết bài học)")
                        .description("Văn phong tự sự gần gũi, chia sẻ trải nghiệm cá nhân thật để truyền cảm hứng và xây dựng lòng tin.")
                        .structure("""
[1. Hook tình huống]: Bắt đầu từ một sự cố, kỷ niệm hoặc tình huống đáng nhớ trong quá khứ.
[2. Diễn biến câu chuyện]: Kể lại những gì đã diễn ra một cách chân thực, cảm xúc và các nút thắt.
[3. Bước ngoặt nhận thức]: Khoảnh khắc nhận ra điều gì đó thay đổi cách nghĩ hoặc cách làm.
[4. 3 bài học đắt giá rút ra]: Đúc kết các nguyên tắc thực tế mà người đọc có thể áp dụng cho chính họ.
[5. Câu hỏi tương tác]: Đặt câu hỏi mở để người đọc cùng chia sẻ góc nhìn hoặc câu chuyện của họ.""")
                        .isDefault(false)
                        .isSystem(true)
                        .build(),

                PostTemplate.builder()
                        .userId(userId)
                        .name("Chia sẻ Mẹo / Listicle (How-to 3-5 bước)")
                        .description("Dạng danh sách gạch đầu dòng rõ ràng, cung cấp giá trị nhanh chóng, dễ đọc lướt và tỷ lệ lưu bài rất cao.")
                        .structure("""
[1. Tiêu đề dạng số]: [X mẹo / X bước để đạt được Y mà không tốn nhiều Z]
[2. Dẫn nhập ngắn gọn]: 1-2 câu nhấn mạnh lý do vì sao phương pháp này lại hiệu quả.
[3. Chi tiết các bước]:
   • Bước 1: [Tên bước] - Hướng dẫn cụ thể 1-2 câu.
   • Bước 2: [Tên bước] - Hướng dẫn cụ thể 1-2 câu.
   • Bước 3: [Tên bước] - Hướng dẫn cụ thể 1-2 câu.
[4. Lưu ý sống còn]: 1 sai lầm phổ biến cần tuyệt đối tránh.
[5. CTA]: Kêu gọi nhấn 'Lưu bài' (Bookmark) và chia sẻ cho bạn bè.""")
                        .isDefault(false)
                        .isSystem(true)
                        .build(),

                PostTemplate.builder()
                        .userId(userId)
                        .name("Myth-Busting (Phá bỏ hiểu lầm & Sự thật bất ngờ)")
                        .description("Đối chiếu giữa hiểu lầm phổ biến và sự thật đúng đắn, khẳng định vị thế chuyên gia.")
                        .structure("""
[1. Hook đối nghịch]: "Nhiều người vẫn lầm tưởng [Quan niệm sai A], nhưng sự thật hoàn toàn ngược lại..."
[2. Bóc tách hiểu lầm]: Giải thích tại sao cách nghĩ cũ lại sai và gây hại thế nào.
[3. Sự thật đúng & Bằng chứng]: Cung cấp góc nhìn đúng đắn dựa trên thực tế hoặc số liệu khách quan.
[4. Lộ trình làm chuẩn]: 2-3 gợi ý hành động đúng đắn.
[5. CTA]: Bạn nghĩ sao về điều này? Hãy để lại quan điểm bên dưới nhé!")
[6. Hashtags]: Hashtags liên quan.""")
                        .isDefault(false)
                        .isSystem(true)
                        .build()
        );

        templateRepository.saveAll(defaults);
    }

    private PostTemplateResponse mapToResponse(PostTemplate t) {
        return PostTemplateResponse.builder()
                .id(t.getId())
                .name(t.getName())
                .description(t.getDescription())
                .structure(t.getStructure())
                .exampleContent(t.getExampleContent())
                .isDefault(t.getIsDefault())
                .isSystem(t.getIsSystem())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }
}
