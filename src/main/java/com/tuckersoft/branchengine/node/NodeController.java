package com.tuckersoft.branchengine.node;

import com.tuckersoft.branchengine.node.dto.CreateNodeRequest;
import com.tuckersoft.branchengine.node.dto.NodeResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/nodes")
@RequiredArgsConstructor
public class NodeController {

    private final NodeService nodeService;

    @PostMapping
    public ResponseEntity<NodeResponse> create(@Valid @RequestBody CreateNodeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(nodeService.create(request));
    }

    @GetMapping
    public List<NodeResponse> list() {
        return nodeService.list();
    }

    @GetMapping("/{id}")
    public NodeResponse getById(@PathVariable Long id) {
        return nodeService.getById(id);
    }
}
