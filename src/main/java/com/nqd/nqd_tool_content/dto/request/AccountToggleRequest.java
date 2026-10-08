package com.nqd.nqd_tool_content.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountToggleRequest {

    @NotNull(message = "Trạng thái bật/tắt không được để trống")
    private Boolean enabled;
}
