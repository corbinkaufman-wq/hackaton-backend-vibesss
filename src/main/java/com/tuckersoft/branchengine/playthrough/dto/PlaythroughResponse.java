package com.tuckersoft.branchengine.playthrough.dto;

import java.time.Instant;

public record PlaythroughResponse(
        Long id,
        String playerTag,
        String ownerEmail,
        String startNodeCode,
        String currentNodeCode,
        Integer lucidity,
        Integer controlLevel,
        String status,
        String endingCode,
        Instant createdAt,
        Instant updatedAt
) {
}
