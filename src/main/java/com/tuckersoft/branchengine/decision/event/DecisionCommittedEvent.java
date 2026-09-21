package com.tuckersoft.branchengine.decision.event;

/**
 * Todo lo que el listener asincrono necesita ya viaja en el evento: en el hilo
 * async ya no hay usuario autenticado ni SecurityContextHolder.
 */
public record DecisionCommittedEvent(
        Long decisionId,
        String recipientEmail,
        String recipientDisplayName,
        String playerTag,
        String branchType,
        String impactLevel,
        String handlerUnit,
        String outcomeCode,
        String sourceNodeCode,
        String resolvedNodeCode,
        String playthroughStatus,
        Integer lucidity,
        Integer controlLevel,
        String endingCode,
        String rawInput,
        String createdAt,
        boolean simulateMailFailure
) {
}
