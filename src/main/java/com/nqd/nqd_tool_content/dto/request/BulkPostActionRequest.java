package com.nqd.nqd_tool_content.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkPostActionRequest {
    @NotBlank
    private String action; // SKIP, DELETE
    @NotEmpty
    private List<UUID> ids;
}
