package com.tuckersoft.branchengine.playthrough.dto;

import java.util.List;

public record PlaythroughPathResponse(
        Long playthroughId,
        String playerTag,
        String status,
        String endingCode,
        String startNodeCode,
        String currentNodeCode,
        List<PathStepResponse> steps
) {
}
