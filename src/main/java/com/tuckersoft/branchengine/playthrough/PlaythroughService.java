package com.tuckersoft.branchengine.playthrough;

import com.tuckersoft.branchengine.common.CurrentUserService;
import com.tuckersoft.branchengine.common.exception.BadRequestException;
import com.tuckersoft.branchengine.common.exception.ConflictException;
import com.tuckersoft.branchengine.common.exception.ForbiddenException;
import com.tuckersoft.branchengine.common.exception.ResourceNotFoundException;
import com.tuckersoft.branchengine.entity.Decision;
import com.tuckersoft.branchengine.entity.Playthrough;
import com.tuckersoft.branchengine.entity.StoryNode;
import com.tuckersoft.branchengine.entity.User;
import com.tuckersoft.branchengine.playthrough.dto.CreatePlaythroughRequest;
import com.tuckersoft.branchengine.playthrough.dto.PathStepResponse;
import com.tuckersoft.branchengine.playthrough.dto.PlaythroughPathResponse;
import com.tuckersoft.branchengine.playthrough.dto.PlaythroughResponse;
import com.tuckersoft.branchengine.repository.DecisionRepository;
import com.tuckersoft.branchengine.repository.PlaythroughRepository;
import com.tuckersoft.branchengine.repository.StoryNodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlaythroughService {

    private final PlaythroughRepository playthroughRepository;
    private final StoryNodeRepository storyNodeRepository;
    private final DecisionRepository decisionRepository;
    private final CurrentUserService currentUserService;

    @Transactional
    public PlaythroughResponse create(CreatePlaythroughRequest request) {
        User owner = currentUserService.getCurrentUser();

        StoryNode startNode = storyNodeRepository.findByNodeCode(request.startNodeCode())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe un StoryNode con nodeCode " + request.startNodeCode()));

        if (playthroughRepository.existsByPlayerTag(request.playerTag())) {
            throw new ConflictException("Ya existe una partida con playerTag " + request.playerTag());
        }

        if (startNode.getCurrentBranches() >= startNode.getBranchCapacity()) {
            throw new BadRequestException("El nodo " + startNode.getNodeCode() + " ya no tiene ramas disponibles");
        }

        Playthrough playthrough = new Playthrough();
        playthrough.setPlayerTag(request.playerTag());
        playthrough.setUser(owner);
        playthrough.setStartNodeCode(startNode.getNodeCode());
        playthrough.setCurrentNode(startNode);
        playthrough.setLucidity(100);
        playthrough.setControlLevel(0);
        playthrough.setStatus("ACTIVA");
        playthrough.setEndingCode(null);

        Instant now = Instant.now();
        playthrough.setCreatedAt(now);
        playthrough.setUpdatedAt(now);

        startNode.setCurrentBranches(startNode.getCurrentBranches() + 1);
        storyNodeRepository.save(startNode);

        playthrough = playthroughRepository.save(playthrough);

        return toResponse(playthrough);
    }

    @Transactional(readOnly = true)
    public List<PlaythroughResponse> listForCurrentUser() {
        User current = currentUserService.getCurrentUser();
        List<Playthrough> playthroughs = currentUserService.isAdmin(current)
                ? playthroughRepository.findAllByOrderByCreatedAtDesc()
                : playthroughRepository.findByUserOrderByCreatedAtDesc(current);
        return playthroughs.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PlaythroughResponse getById(Long id) {
        return toResponse(findVisible(id));
    }

    @Transactional(readOnly = true)
    public PlaythroughPathResponse getPath(Long id) {
        Playthrough playthrough = findVisible(id);

        List<Decision> resolved = decisionRepository.findByPlaythroughOrderByCreatedAtAsc(playthrough).stream()
                .filter(d -> d.getResolvedNodeCode() != null)
                .toList();

        List<PathStepResponse> steps = new ArrayList<>();
        int order = 1;
        for (Decision d : resolved) {
            steps.add(new PathStepResponse(
                    order++,
                    d.getId(),
                    d.getNode().getNodeCode(),
                    d.getResolvedNodeCode(),
                    d.getBranchType(),
                    d.getImpactLevel(),
                    d.getCreatedAt()
            ));
        }

        return new PlaythroughPathResponse(
                playthrough.getId(),
                playthrough.getPlayerTag(),
                playthrough.getStatus(),
                playthrough.getEndingCode(),
                playthrough.getStartNodeCode(),
                playthrough.getCurrentNode().getNodeCode(),
                steps
        );
    }

    /**
     * Usado por DecisionService. La regla de propiedad para ESCRIBIR no tiene
     * excepcion de administrador: "el administrador supervisa, no juega".
     */
    @Transactional(readOnly = true)
    public Playthrough findOwnedStrict(Long id) {
        User current = currentUserService.getCurrentUser();
        Playthrough playthrough = playthroughRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe una partida con id " + id));
        if (!playthrough.getUser().getId().equals(current.getId())) {
            throw new ForbiddenException("Esta partida no te pertenece");
        }
        return playthrough;
    }

    private Playthrough findVisible(Long id) {
        User current = currentUserService.getCurrentUser();
        Playthrough playthrough = playthroughRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe una partida con id " + id));
        boolean isOwner = playthrough.getUser().getId().equals(current.getId());
        if (!isOwner && !currentUserService.isAdmin(current)) {
            throw new ForbiddenException("Esta partida no te pertenece");
        }
        return playthrough;
    }

    private PlaythroughResponse toResponse(Playthrough p) {
        return new PlaythroughResponse(
                p.getId(),
                p.getPlayerTag(),
                p.getUser().getEmail(),
                p.getStartNodeCode(),
                p.getCurrentNode().getNodeCode(),
                p.getLucidity(),
                p.getControlLevel(),
                p.getStatus(),
                p.getEndingCode(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
