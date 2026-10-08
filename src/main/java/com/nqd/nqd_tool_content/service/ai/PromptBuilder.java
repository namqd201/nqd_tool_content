package com.nqd.nqd_tool_content.service.ai;

import com.nqd.nqd_tool_content.service.ai.dto.AIContentRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PromptBuilder {

    public String buildPostGenerationPrompt(AIContentRequest req) {
        StringBuilder sb = new StringBuilder();
        sb.append("Bạn là một chuyên gia sáng tạo nội dung mạng xã hội hàng đầu.\n");
        sb.append("Nhiệm vụ: Viết bài đăng tối ưu hóa riêng cho từng nền tảng mạng xã hội dựa trên thông tin sau.\n\n");

        sb.append("--- THÔNG TIN CHỦ ĐỀ & KẾ HOẠCH ---\n");
        sb.append("Chủ đề: ").append(req.getTopic()).append("\n");
        if (req.getAngle() != null && !req.getAngle().isBlank()) {
            sb.append("Góc nhìn (Angle): ").append(req.getAngle()).append("\n");
        }
        if (req.getInstructions() != null && !req.getInstructions().isBlank()) {
            sb.append("Chỉ dẫn bổ sung: ").append(req.getInstructions()).append("\n");
        }
        if (req.getTone() != null && !req.getTone().isBlank()) {
            sb.append("Giọng văn (Tone): ").append(req.getTone()).append("\n");
        }
        sb.append("Ngôn ngữ: ").append(req.getLanguage() != null ? req.getLanguage() : "vi").append("\n");

        if (req.getForbiddenWords() != null && !req.getForbiddenWords().isEmpty()) {
            sb.append("Từ cấm tuyệt đối không dùng: ").append(String.join(", ", req.getForbiddenWords())).append("\n");
        }

        if (req.getPreferredHashtags() != null && !req.getPreferredHashtags().isEmpty()) {
            sb.append("Hashtag ưu tiên: ").append(String.join(" ", req.getPreferredHashtags())).append("\n");
        }

        if (req.getCustomPrompt() != null && !req.getCustomPrompt().isBlank()) {
            sb.append("\n--- YÊU CẦU ĐẶC BIỆT / CHỈ ĐẠO CỦA NGƯỜI DÙNG ---\n");
            sb.append(req.getCustomPrompt()).append("\n");
            sb.append("BẮT BUỘC TUÂN THỦ: Bạn phải điều chỉnh cấu trúc, nội dung và phong cách viết bám sát 100% chỉ đạo trên của người dùng!\n");
        }

        if (req.getRecentPosts() != null && !req.getRecentPosts().isEmpty()) {
            sb.append("\n--- CÁC BÀI VIẾT ĐÃ TẠO TRƯỚC ĐÓ TRONG KẾ HOẠCH (BẮT BUỘC CHỐNG TRÙNG LẶP) ---\n");
            for (int i = 0; i < req.getRecentPosts().size(); i++) {
                sb.append("- Bài cũ #").append(i + 1).append(": ").append(req.getRecentPosts().get(i)).append("\n");
            }
            sb.append("NGUYÊN TẮC CHỐNG TRÙNG LẶP TUYỆT ĐỐI:\n");
            sb.append("• KHÔNG dùng lại câu hook/mở đầu tương tự các bài cũ trên.\n");
            sb.append("• KHÔNG lặp lại cùng một ví dụ, mẫu câu, hay lời kêu gọi hành động (CTA) giống hệt.\n");
            sb.append("• Bắt buộc khai thác góc tiếp cận mới, văn phong cuốn hút và cung cấp giá trị độc đáo khác biệt.\n");
        }

        sb.append("\n--- CÁC NỀN TẢNG YÊU CẦU ---\n");
        for (String p : req.getPlatforms()) {
            sb.append("- ").append(p);
            if ("X".equalsIgnoreCase(p)) sb.append(" (Tối đa 280 ký tự, súc tích, ấn tượng)");
            else if ("THREADS".equalsIgnoreCase(p)) sb.append(" (Tối đa 500 ký tự, gần gũi, chia sẻ)");
            else if ("FACEBOOK".equalsIgnoreCase(p)) sb.append(" (Đầy đủ, dẫn dắt câu chuyện cuốn hút, kêu gọi tương tác)");
            else if ("LINKEDIN".equalsIgnoreCase(p)) sb.append(" (Chuyên nghiệp, góc nhìn chuyên môn, giá trị thực tế)");
            sb.append("\n");
        }

        sb.append("\n--- NGUYÊN TẮC BẮT BUỘC ---\n");
        sb.append("1. Tuyệt đối không bịa đặt số liệu hay thông tin không có trong brief.\n");
        sb.append("2. Mỗi nền tảng phải có nội dung và văn phong phù hợp với đặc thù nền tảng đó.\n");
        sb.append("3. Không để lại bất kỳ placeholder nào như {{...}}, [...] hoặc TODO.\n");
        if (req.isGenerateImagePrompt()) {
            sb.append("4. Tạo một mô tả hình ảnh cực kỳ chi tiết bằng tiếng Anh (imagePrompt) phản ánh trực tiếp nội dung bài viết:\n");
            sb.append("   - Mô tả cảnh tượng cụ thể: chủ thể chính (subject), môi trường xung quanh (environment), ánh sáng, góc chụp.\n");
            sb.append("   - Phong cách: photorealistic, modern aesthetic, vibrant colors, cinematic lighting.\n");
            sb.append("   - QUAN TRỌNG: BẮT BUỘC thêm 'strictly no text, no letters, no watermark' để tránh ảnh sinh ra chữ rác.\n");
            sb.append("   - Tuyệt đối không viết mô tả trừu tượng chung chung như 'illustration of topic'.\n");
        }

        sb.append("\n--- ĐỊNH DẠNG ĐẦU RA ---\n");
        sb.append("Trả về DUY NHẤT một JSON hợp lệ (không kèm markdown ```json bọc ngoài) theo mẫu:\n");
        sb.append("{\n");
        sb.append("  \"items\": [\n");
        for (int i = 0; i < req.getPlatforms().size(); i++) {
            String p = req.getPlatforms().get(i);
            sb.append("    { \"platform\": \"").append(p).append("\", \"content\": \"...\", \"hashtags\": [\"#...\"] }");
            if (i < req.getPlatforms().size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ],\n");
        sb.append("  \"imagePrompt\": \"").append(req.isGenerateImagePrompt() ? "A professional cinematic photo showing a modern workspace with team collaborating on creative project, natural sunlight, highly detailed, 8k, strictly no text, no watermark" : "").append("\"\n");
        sb.append("}\n");

        return sb.toString();
    }

    public String buildPostChatPrompt(
            String message,
            String currentContent,
            String platform,
            String topic,
            String angle,
            String tone,
            java.util.List<com.nqd.nqd_tool_content.dto.request.ChatMessageDTO> history
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("Bạn là AI Content Strategist & Copywriter chuyên nghiệp hàng đầu.\n");
        sb.append("Nhiệm vụ: Bạn đang trò chuyện và cùng người dùng sáng tạo/tinh chỉnh bài viết cho mạng xã hội ").append(platform != null ? platform : "FACEBOOK").append(".\n\n");
        sb.append("--- NGỮ CẢNH BÀI VIẾT HIỆN TẠI ---\n");
        sb.append("Chủ đề: ").append(topic != null ? topic : "Nội dung mạng xã hội").append("\n");
        if (angle != null && !angle.isBlank()) sb.append("Góc nhìn (Angle): ").append(angle).append("\n");
        if (tone != null && !tone.isBlank()) sb.append("Giọng văn (Tone): ").append(tone).append("\n");
        if (currentContent != null && !currentContent.isBlank()) {
            sb.append("Nội dung bài viết hiện tại:\n\"\"\"\n").append(currentContent).append("\n\"\"\"\n\n");
        }

        if (history != null && !history.isEmpty()) {
            sb.append("--- LỊCH SỬ TRAO ĐỔI TRƯỚC ĐÓ ---\n");
            for (var m : history) {
                sb.append(m.getRole() != null && m.getRole().equalsIgnoreCase("user") ? "Người dùng: " : "AI: ");
                sb.append(m.getContent()).append("\n");
            }
            sb.append("\n");
        }

        sb.append("--- TIN NHẮN MỚI NHẤT CỦA NGƯỜI DÙNG ---\n");
        sb.append(message).append("\n\n");

        sb.append("--- NGUYÊN TẮC PHẢN HỒI ---\n");
        sb.append("1. Trò chuyện tự nhiên, thân thiện và lắng nghe người dùng bằng tiếng Việt.\n");
        sb.append("2. Nếu người dùng yêu cầu sửa bài, viết lại, rút gọn, thêm ý: Hãy giải thích ngắn gọn cách bạn điều chỉnh và cung cấp bản bài viết hoàn chỉnh mới nhất trong trường 'suggestedContent'.\n");
        sb.append("3. Nếu người dùng thắc mắc, hỏi ý kiến hoặc brief chưa rõ: Hãy trả lời nhiệt tình, gợi ý thêm ý tưởng hoặc hỏi lại những điểm bạn cần người dùng làm rõ.\n");
        sb.append("4. Trả về DUY NHẤT một JSON hợp lệ (không kèm markdown ```json bọc ngoài) theo mẫu:\n");
        sb.append("{\n");
        sb.append("  \"reply\": \"Lời phản hồi đối thoại của bạn với người dùng...\",\n");
        sb.append("  \"suggestedContent\": \"Nội dung bài viết hoàn chỉnh mới (hoặc null nếu chỉ đang trả lời giải đáp)\",\n");
        sb.append("  \"suggestedHashtags\": [\"#tag1\", \"#tag2\"]\n");
        sb.append("}\n");

        return sb.toString();
    }

    public String buildPlanAnglesPrompt(String topic, int count, String language) {
        return "Hãy đề xuất " + count + " góc nội dung (angles) khác nhau và sáng tạo cho chủ đề: \"" + topic + "\".\n" +
                "Mỗi góc nội dung phải có cách tiếp cận riêng (ví dụ: mẹo thực tế, câu chuyện, hỏi đáp, sai lầm phổ biến, phân tích xu hướng...).\n" +
                "Ngôn ngữ: " + (language != null ? language : "vi") + ".\n" +
                "Trả về định dạng JSON array dạng: [\"Góc 1: ...\", \"Góc 2: ...\"]";
    }
}
