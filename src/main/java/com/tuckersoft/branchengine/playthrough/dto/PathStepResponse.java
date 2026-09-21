package com.tuckersoft.branchengine.playthrough.dto;

import java.time.Instant;

public record PathStepResponse(
        int order,
        Long decisionId,
        String fromNodeCode,
        String toNodeCode,
        String branchType,
        String impactLevel,
        Instant createdAt
) {
}
