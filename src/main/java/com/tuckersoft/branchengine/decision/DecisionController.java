package com.tuckersoft.branchengine.decision;

import com.tuckersoft.branchengine.decision.dto.CreateDecisionRequest;
import com.tuckersoft.branchengine.decision.dto.DecisionResponse;
import com.tuckersoft.branchengine.decision.dto.PageResponse;
import com.tuckersoft.branchengine.decision.dto.RealityLogResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/decisions")
@RequiredArgsConstructor
public class DecisionController {

    private final DecisionService decisionService;

    @PostMapping
    public ResponseEntity<DecisionResponse> decide(
            @Valid @RequestBody CreateDecisionRequest request,
            @RequestHeader(name = "X-Bandersnatch-Simulate", required = false) String simulate) {
        return ResponseEntity.status(HttpStatus.CREATED).body(decisionService.decide(request, simulate));
    }

    @GetMapping
    public ResponseEntity<PageResponse<DecisionResponse>> list(
            @RequestParam(required = false) String branchType,
            @RequestParam(required = false) String impactLevel,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long playthroughId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(decisionService.list(branchType, impactLevel, status, playthroughId, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DecisionResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(decisionService.getById(id));
    }

    @GetMapping("/{id}/reality-logs")
    public ResponseEntity<List<RealityLogResponse>> getRealityLogs(@PathVariable Long id) {
        return ResponseEntity.ok(decisionService.getRealityLogs(id));
    }
}
