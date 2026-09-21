package com.tuckersoft.branchengine.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "story_nodes")
@Getter
@Setter
@NoArgsConstructor
public class StoryNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nodeCode;

    private String title;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String sceneText;

    private Integer branchCapacity;

    private Integer currentBranches = 0;

    private String primaryBranchCode;

    private String glitchBranchCode;

    private Instant createdAt;

    @OneToMany(mappedBy = "currentNode")
    private List<Playthrough> playthroughs = new ArrayList<>();

    @OneToMany(mappedBy = "node")
    private List<Decision> decisions = new ArrayList<>();
}
