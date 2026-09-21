package com.tuckersoft.branchengine.decision.dto;

import java.time.Instant;

public record RealityLogResponse(
        Long id,
        Long decisionId,
        String recipientEmail,
        String subject,
        String logStatus,
        String errorMessage,
        Instant sentAt,
        Instant createdAt
) {
}
