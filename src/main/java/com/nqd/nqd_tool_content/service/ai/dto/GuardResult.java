package com.nqd.nqd_tool_content.service.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuardResult {
    public enum Status {
        PASS,
        PASS_WITH_WARNINGS,
        REJECT
    }

    private Status status;
    @Builder.Default
    private List<String> warnings = new ArrayList<>();
    @Builder.Default
    private List<String> errors = new ArrayList<>();

    public boolean isAllowed() {
        return status == Status.PASS || status == Status.PASS_WITH_WARNINGS;
    }

    public static GuardResult pass() {
        return GuardResult.builder().status(Status.PASS).build();
    }

    public static GuardResult reject(String reason) {
        GuardResult gr = GuardResult.builder().status(Status.REJECT).build();
        gr.getErrors().add(reason);
        return gr;
    }

    public static GuardResult warning(String warning) {
        GuardResult gr = GuardResult.builder().status(Status.PASS_WITH_WARNINGS).build();
        gr.getWarnings().add(warning);
        return gr;
    }
}
