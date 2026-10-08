package com.nqd.nqd_tool_content.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePlanRequest {

    @NotBlank(message = "Tên kế hoạch không được để trống")
    private String name;

    @NotBlank(message = "Chủ đề chính không được để trống")
    private String topic;

    private String instructions;

    @Builder.Default
    private String language = "vi";

    private String tone;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDate startDate;

    @NotNull(message = "Ngày kết thúc không được để trống")
    private LocalDate endDate;

    @Builder.Default
    private String timezone = "Asia/Ho_Chi_Minh";

    @NotNull(message = "Số bài mỗi ngày không được để trống")
    @Min(value = 1, message = "Tối thiểu 1 bài/ngày")
    @Max(value = 10, message = "Tối đa 10 bài/ngày")
    @Builder.Default
    private Integer postsPerDay = 1;

    @Builder.Default
    private String timeMode = "FIXED_TIMES"; // FIXED_TIMES, WINDOWS

    @Builder.Default
    private List<String> timeSlots = new ArrayList<>();

    @NotEmpty(message = "Vui lòng chọn ít nhất một kênh đăng bài")
    @Builder.Default
    private List<UUID> targetAccountIds = new ArrayList<>();

    @Builder.Default
    private Boolean includeImage = true;

    private String imageStyle;

    @Builder.Default
    private String imageFailurePolicy = "POST_WITHOUT_IMAGE";

    @Builder.Default
    private String contentMode = "ADAPT_SAME_IDEA";
}
