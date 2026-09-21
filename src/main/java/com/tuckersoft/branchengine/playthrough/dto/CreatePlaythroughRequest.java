package com.tuckersoft.branchengine.playthrough.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePlaythroughRequest(
        @NotBlank @Size(min = 2, max = 40) String playerTag,
        @NotBlank String startNodeCode
) {
}
