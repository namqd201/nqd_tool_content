package com.nqd.nqd_tool_content.service.plan;

import org.springframework.stereotype.Component;

import java.time.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class SlotCalculator {

    /**
     * Tính toán danh sách các slot thời gian đăng bài theo cấu hình của Plan.
     */
    public List<CalculatedSlot> calculateSlots(
            LocalDate startDate,
            LocalDate endDate,
            String timezoneStr,
            int postsPerDay,
            String timeMode,
            List<String> rawTimeSlots,
            int generateLeadHours
    ) {
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            return Collections.emptyList();
        }

        ZoneId zoneId;
        try {
            zoneId = ZoneId.of(timezoneStr != null ? timezoneStr : "Asia/Ho_Chi_Minh");
        } catch (Exception e) {
            zoneId = ZoneId.of("Asia/Ho_Chi_Minh");
        }

        // Chuẩn bị danh sách giờ trong ngày
        List<LocalTime> dailyTimes = new ArrayList<>();
        if (rawTimeSlots != null && !rawTimeSlots.isEmpty()) {
            for (String s : rawTimeSlots) {
                try {
                    dailyTimes.add(LocalTime.parse(s.trim()));
                } catch (Exception ignored) {
                }
            }
        }

        // Nếu danh sách rỗng hoặc ít hơn postsPerDay, tự động gán các giờ mặc định
        if (dailyTimes.isEmpty()) {
            if (postsPerDay == 1) {
                dailyTimes.add(LocalTime.of(9, 0));
            } else if (postsPerDay == 2) {
                dailyTimes.add(LocalTime.of(9, 0));
                dailyTimes.add(LocalTime.of(19, 0));
            } else if (postsPerDay == 3) {
                dailyTimes.add(LocalTime.of(9, 0));
                dailyTimes.add(LocalTime.of(14, 0));
                dailyTimes.add(LocalTime.of(20, 0));
            } else {
                int intervalHours = Math.max(1, 14 / postsPerDay);
                for (int i = 0; i < postsPerDay; i++) {
                    dailyTimes.add(LocalTime.of(8 + (i * intervalHours), 0));
                }
            }
        }

        Collections.sort(dailyTimes);

        List<CalculatedSlot> result = new ArrayList<>();
        LocalDate curDate = startDate;

        while (!curDate.isAfter(endDate)) {
            int slotIdx = 1;
            for (LocalTime time : dailyTimes) {
                if (slotIdx > postsPerDay) break;

                // Giờ hẹn đăng bài chính xác theo ngày và giờ người dùng đã thiết lập
                LocalDateTime scheduledAt = LocalDateTime.of(curDate, time);
                LocalDateTime generateAt = scheduledAt.minusHours(generateLeadHours);

                String slotKey = curDate.toString() + "#" + slotIdx;

                result.add(CalculatedSlot.builder()
                        .slotKey(slotKey)
                        .localDate(curDate)
                        .localTime(time)
                        .scheduledAt(scheduledAt)
                        .generateAt(generateAt)
                        .build());

                slotIdx++;
            }
            curDate = curDate.plusDays(1);
        }

        return result;
    }
}
