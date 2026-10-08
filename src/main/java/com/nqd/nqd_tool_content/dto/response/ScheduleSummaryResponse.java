package com.nqd.nqd_tool_content.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleSummaryResponse {
    private long publishedToday;
    private long scheduledNext24h;
    private long needsAttention; // FAILED + NEEDS_REVIEW + GENERATION_FAILED + MISSED
}
