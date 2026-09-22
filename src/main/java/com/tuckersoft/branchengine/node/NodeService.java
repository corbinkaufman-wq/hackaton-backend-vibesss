package com.tuckersoft.branchengine.node;

import com.tuckersoft.branchengine.common.exception.ConflictException;
import com.tuckersoft.branchengine.common.exception.ResourceNotFoundException;
import com.tuckersoft.branchengine.entity.StoryNode;
import com.tuckersoft.branchengine.node.dto.CreateNodeRequest;
import com.tuckersoft.branchengine.node.dto.NodeResponse;
import com.tuckersoft.branchengine.repository.StoryNodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NodeService {

    private final StoryNodeRepository storyNodeRepository;

    @Transactional
    public NodeResponse create(CreateNodeRequest request) {
        if (storyNodeRepository.existsByNodeCode(request.nodeCode())) {
            throw new ConflictException("Ya existe un nodo con nodeCode " + request.nodeCode());
        }
        StoryNode node = new StoryNode();
        node.setNodeCode(request.nodeCode());
        node.setTitle(request.title());
        node.setSceneText(request.sceneText());
        node.setBranchCapacity(request.branchCapacity());
        node.setCurrentBranches(0);
        node.setPrimaryBranchCode(request.primaryBranchCode());
        node.setGlitchBranchCode(request.glitchBranchCode());
        node.setCreatedAt(Instant.now());
        return toResponse(storyNodeRepository.save(node));
    }

    public List<NodeResponse> list() {
        return storyNodeRepository.findAll().stream().map(this::toResponse).toList();
    }

    public NodeResponse getById(Long id) {
        StoryNode node = storyNodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe un nodo con id " + id));
        return toResponse(node);
    }

    private NodeResponse toResponse(StoryNode n) {
        return new NodeResponse(n.getId(), n.getNodeCode(), n.getTitle(), n.getSceneText(),
                n.getBranchCapacity(), n.getCurrentBranches(), n.getPrimaryBranchCode(),
                n.getGlitchBranchCode(), n.getCreatedAt());
    }
}
