package com.tuckersoft.branchengine.decision;

import com.tuckersoft.branchengine.common.CurrentUserService;
import com.tuckersoft.branchengine.common.exception.ConflictException;
import com.tuckersoft.branchengine.common.exception.ForbiddenException;
import com.tuckersoft.branchengine.common.exception.ResourceNotFoundException;
import com.tuckersoft.branchengine.decision.dto.CreateDecisionRequest;
import com.tuckersoft.branchengine.decision.dto.DecisionResponse;
import com.tuckersoft.branchengine.decision.dto.PageResponse;
import com.tuckersoft.branchengine.decision.dto.RealityLogResponse;
import com.tuckersoft.branchengine.decision.event.DecisionCommittedEvent;
import com.tuckersoft.branchengine.entity.Decision;
import com.tuckersoft.branchengine.entity.Playthrough;
import com.tuckersoft.branchengine.entity.RealityLog;
import com.tuckersoft.branchengine.entity.StoryNode;
import com.tuckersoft.branchengine.entity.User;
import com.tuckersoft.branchengine.repository.DecisionRepository;
import com.tuckersoft.branchengine.repository.PlaythroughRepository;
import com.tuckersoft.branchengine.repository.RealityLogRepository;
import com.tuckersoft.branchengine.repository.StoryNodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DecisionService {

    private final DecisionRepository decisionRepository;
    private final PlaythroughRepository playthroughRepository;
    private final StoryNodeRepository storyNodeRepository;
    private final RealityLogRepository realityLogRepository;
    private final CurrentUserService currentUserService;
    private final DecisionClassifier classifier;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Orden exacto del enunciado:
     * 1. usuario autenticado + dueno de la partida (admin incluido: sin excepcion)
     * 2. partida ACTIVA
     * 3. clasificar y derivar handlerUnit/outcomeCode
     * 4. ENTRADA_CORRUPTA -> guarda con ERROR, no toca la partida, no publica evento, 201
     * 5. aplica stats, resuelve nodo destino y estado de la partida
     * 6-7. guarda Playthrough y Decision (REGISTRADA)
     * 8. publica DecisionCommittedEvent
     * 9. retorna 201
     */
    @Transactional
    public DecisionResponse decide(CreateDecisionRequest request, String simulateHeader) {
        Playthrough playthrough = playthroughRepository.findById(request.playthroughId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe una partida con id " + request.playthroughId()));

        User current = currentUserService.getCurrentUser();
        if (!playthrough.getUser().getId().equals(current.getId())) {
            throw new ForbiddenException("Esta partida no te pertenece");
        }
        if (!"ACTIVA".equals(playthrough.getStatus())) {
            throw new ConflictException("La partida ya esta FINALIZADA");
        }

        StoryNode sourceNode = playthrough.getCurrentNode();
        String branchType = classifier.classify(request.rawInput());
        BranchMetadata.Meta meta = BranchMetadata.forBranchType(branchType);

        Decision decision = new Decision();
        decision.setPlaythrough(playthrough);
        decision.setNode(sourceNode);
        decision.setRawInput(request.rawInput());
        decision.setBranchType(branchType);
        decision.setImpactLevel(request.impactLevel());
        decision.setHandlerUnit(meta.handlerUnit());
        decision.setOutcomeCode(meta.outcomeCode());
        Instant now = Instant.now();
        decision.setCreatedAt(now);
        decision.setUpdatedAt(now);

        if ("ENTRADA_CORRUPTA".equals(branchType)) {
            decision.setResolvedNodeCode(null);
            decision.setStatus("ERROR");
            decision = decisionRepository.save(decision);
            return toResponse(decision, playthrough);
        }

        ImpactStats.Delta delta = ImpactStats.forImpact(request.impactLevel());
        int newLucidity = clamp(playthrough.getLucidity() + delta.lucidity());
        int newControl = clamp(playthrough.getControlLevel() + delta.controlLevel());

        boolean useGlitch = "RUPTURA_CUARTA_PARED".equals(branchType) || "CRITICO".equals(request.impactLevel());
        String targetCode = useGlitch ? sourceNode.getGlitchBranchCode() : sourceNode.getPrimaryBranchCode();
        decision.setResolvedNodeCode(targetCode);

        StoryNode targetNode = targetCode == null ? null
                : storyNodeRepository.findByNodeCode(targetCode).orElse(null);

        playthrough.setLucidity(newLucidity);
        playthrough.setControlLevel(newControl);
        playthrough.setUpdatedAt(now);

        if (newControl >= 100) {
            playthrough.setStatus("FINALIZADA");
            playthrough.setEndingCode("ENDING_PAC_SYMBOL");
        } else if (newLucidity <= 0) {
            playthrough.setStatus("FINALIZADA");
            playthrough.setEndingCode("ENDING_WHITE_BEAR");
        } else if (targetNode == null) {
            playthrough.setStatus("FINALIZADA");
            playthrough.setEndingCode("ENDING_NETFLIX_CUT");
        } else {
            playthrough.setStatus("ACTIVA");
            playthrough.setCurrentNode(targetNode);
        }

        decision.setStatus("REGISTRADA");
        playthroughRepository.save(playthrough);
        decision = decisionRepository.save(decision);

        publishEvent(decision, playthrough, simulateHeader);

        return toResponse(decision, playthrough);
    }

    @Transactional(readOnly = true)
    public PageResponse<DecisionResponse> list(String branchType, String impactLevel, String status,
                                                Long playthroughId, int page, int size) {
        User current = currentUserService.getCurrentUser();

        Specification<Decision> spec = Specification.where(null);
        if (!currentUserService.isAdmin(current)) {
            Long userId = current.getId();
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("playthrough").get("user").get("id"), userId));
        }
        if (branchType != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("branchType"), branchType));
        }
        if (impactLevel != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("impactLevel"), impactLevel));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (playthroughId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("playthrough").get("id"), playthroughId));
        }

        Page<Decision> result = decisionRepository.findAll(spec, PageRequest.of(page, size));
        List<DecisionResponse> content = result.getContent().stream()
                .map(d -> toResponse(d, d.getPlaythrough()))
                .toList();

        return new PageResponse<>(content, result.getTotalElements(), result.getTotalPages(), page, size);
    }

    @Transactional(readOnly = true)
    public DecisionResponse getById(Long id) {
        Decision decision = findVisible(id);
        return toResponse(decision, decision.getPlaythrough());
    }

    @Transactional(readOnly = true)
    public List<RealityLogResponse> getRealityLogs(Long id) {
        Decision decision = findVisible(id);
        return realityLogRepository.findByDecisionOrderByCreatedAtAsc(decision).stream()
                .map(this::toLogResponse)
                .toList();
    }

    private Decision findVisible(Long id) {
        User current = currentUserService.getCurrentUser();
        Decision decision = decisionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe una decision con id " + id));
        boolean isOwner = decision.getPlaythrough().getUser().getId().equals(current.getId());
        if (!isOwner && !currentUserService.isAdmin(current)) {
            throw new ForbiddenException("Esta decision no te pertenece");
        }
        return decision;
    }

    private void publishEvent(Decision decision, Playthrough playthrough, String simulateHeader) {
        User owner = playthrough.getUser();
        boolean simulateFailure = "MAIL_FAILURE".equals(simulateHeader);

        DecisionCommittedEvent event = new DecisionCommittedEvent(
                decision.getId(),
                owner.getEmail(),
                owner.getDisplayName(),
                playthrough.getPlayerTag(),
                decision.getBranchType(),
                decision.getImpactLevel(),
                decision.getHandlerUnit(),
                decision.getOutcomeCode(),
                decision.getNode().getNodeCode(),
                decision.getResolvedNodeCode(),
                playthrough.getStatus(),
                playthrough.getLucidity(),
                playthrough.getControlLevel(),
                playthrough.getEndingCode(),
                decision.getRawInput(),
                DateTimeFormatter.ISO_INSTANT.format(decision.getCreatedAt()),
                simulateFailure
        );
        eventPublisher.publishEvent(event);
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private DecisionResponse toResponse(Decision d, Playthrough p) {
        return new DecisionResponse(
                d.getId(),
                p.getId(),
                p.getPlayerTag(),
                d.getNode().getNodeCode(),
                d.getResolvedNodeCode(),
                d.getRawInput(),
                d.getBranchType(),
                d.getImpactLevel(),
                d.getHandlerUnit(),
                d.getOutcomeCode(),
                d.getStatus(),
                p.getStatus(),
                p.getLucidity(),
                p.getControlLevel(),
                p.getEndingCode(),
                d.getCreatedAt(),
                d.getUpdatedAt()
        );
    }

    private RealityLogResponse toLogResponse(RealityLog log) {
        return new RealityLogResponse(
                log.getId(),
                log.getDecision().getId(),
                log.getRecipientEmail(),
                log.getSubject(),
                log.getLogStatus(),
                log.getErrorMessage(),
                log.getSentAt(),
                log.getCreatedAt()
        );
    }
}
