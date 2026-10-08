package com.nqd.nqd_tool_content.service.plan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalculatedSlot {
    private String slotKey;
    private LocalDate localDate;
    private LocalTime localTime;
    private LocalDateTime scheduledAt; // UTC
    private LocalDateTime generateAt;  // UTC
}
