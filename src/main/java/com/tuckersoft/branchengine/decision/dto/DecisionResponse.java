package com.tuckersoft.branchengine.decision.dto;

import java.time.Instant;

public record DecisionResponse(
        Long id,
        Long playthroughId,
        String playerTag,
        String sourceNodeCode,
        String resolvedNodeCode,
        String rawInput,
        String branchType,
        String impactLevel,
        String handlerUnit,
        String outcomeCode,
        String status,
        String playthroughStatus,
        Integer lucidity,
        Integer controlLevel,
        String endingCode,
        Instant createdAt,
        Instant updatedAt
) {
}
