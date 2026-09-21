package com.tuckersoft.branchengine.playthrough;

import com.tuckersoft.branchengine.playthrough.dto.CreatePlaythroughRequest;
import com.tuckersoft.branchengine.playthrough.dto.PlaythroughPathResponse;
import com.tuckersoft.branchengine.playthrough.dto.PlaythroughResponse;
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
@RequestMapping("/api/v1/playthroughs")
@RequiredArgsConstructor
public class PlaythroughController {

    private final PlaythroughService playthroughService;

    @PostMapping
    public ResponseEntity<PlaythroughResponse> create(@Valid @RequestBody CreatePlaythroughRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(playthroughService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<PlaythroughResponse>> list() {
        return ResponseEntity.ok(playthroughService.listForCurrentUser());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlaythroughResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(playthroughService.getById(id));
    }

    @GetMapping("/{id}/path")
    public ResponseEntity<PlaythroughPathResponse> getPath(@PathVariable Long id) {
        return ResponseEntity.ok(playthroughService.getPath(id));
    }
}
