package com.tuckersoft.branchengine.node.dto;

import java.time.Instant;

public record NodeResponse(
        Long id,
        String nodeCode,
        String title,
        String sceneText,
        Integer branchCapacity,
        Integer currentBranches,
        String primaryBranchCode,
        String glitchBranchCode,
        Instant createdAt
) {
}
