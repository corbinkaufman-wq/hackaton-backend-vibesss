package com.tuckersoft.branchengine.decision;

import com.tuckersoft.branchengine.common.CurrentUserService;
import com.tuckersoft.branchengine.decision.dto.CreateDecisionRequest;
import com.tuckersoft.branchengine.decision.dto.DecisionResponse;
import com.tuckersoft.branchengine.entity.Playthrough;
import com.tuckersoft.branchengine.entity.StoryNode;
import com.tuckersoft.branchengine.entity.User;
import com.tuckersoft.branchengine.repository.DecisionRepository;
import com.tuckersoft.branchengine.repository.PlaythroughRepository;
import com.tuckersoft.branchengine.repository.RealityLogRepository;
import com.tuckersoft.branchengine.repository.StoryNodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Corren sin PostgreSQL ni red: todos los repositorios y el publisher de
 * eventos estan mockeados con Mockito.
 */
@ExtendWith(MockitoExtension.class)
class DecisionServiceTest {

    @Mock
    private DecisionRepository decisionRepository;
    @Mock
    private PlaythroughRepository playthroughRepository;
    @Mock
    private StoryNodeRepository storyNodeRepository;
    @Mock
    private RealityLogRepository realityLogRepository;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private DecisionService service;
    private final DecisionClassifier classifier = new DecisionClassifier();

    private User user;
    private StoryNode node;
    private Playthrough playthrough;

    @BeforeEach
    void setUp() {
        service = new DecisionService(decisionRepository, playthroughRepository, storyNodeRepository,
                realityLogRepository, currentUserService, classifier, eventPublisher);

        user = new User();
        user.setId(1L);
        user.setEmail("ada@tuckersoft.test");
        user.setDisplayName("Ada Lovelace");
        user.setRole("ROLE_USER");

        node = new StoryNode();
        node.setId(1L);
        node.setNodeCode("NODE-CEREAL");
        node.setPrimaryBranchCode("NODE-BUS");
        node.setGlitchBranchCode("NODE-ESPEJO");
        node.setBranchCapacity(5);
        node.setCurrentBranches(0);

        playthrough = new Playthrough();
        playthrough.setId(10L);
        playthrough.setPlayerTag("STEFAN-01");
        playthrough.setUser(user);
        playthrough.setStartNodeCode("NODE-CEREAL");
        playthrough.setCurrentNode(node);
        playthrough.setLucidity(100);
        playthrough.setControlLevel(0);
        playthrough.setStatus("ACTIVA");
        playthrough.setCreatedAt(Instant.now());
        playthrough.setUpdatedAt(Instant.now());

        lenient().when(currentUserService.getCurrentUser()).thenReturn(user);
        lenient().when(currentUserService.isAdmin(user)).thenReturn(false);
        lenient().when(playthroughRepository.findById(10L)).thenReturn(Optional.of(playthrough));
        lenient().when(decisionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void reglaDeCamaraGanaSobreDestruye() {
        CreateDecisionRequest request = new CreateDecisionRequest(10L,
                "Stefan destruye la camara que lo estaba grabando.", "LEVE");

        DecisionResponse response = service.decide(request, null);

        assertThat(response.branchType()).isEqualTo("RUPTURA_CUARTA_PARED");
    }

    @Test
    void entradaSinLetrasEsCorruptaYNoTocaLaPartida() {
        CreateDecisionRequest request = new CreateDecisionRequest(10L, "%%% 0101 ### @@ 11", "CRITICO");

        DecisionResponse response = service.decide(request, null);

        assertThat(response.branchType()).isEqualTo("ENTRADA_CORRUPTA");
        assertThat(response.status()).isEqualTo("ERROR");
        assertThat(response.resolvedNodeCode()).isNull();
        assertThat(response.lucidity()).isEqualTo(100);
        assertThat(response.controlLevel()).isEqualTo(0);
        assertThat(playthrough.getStatus()).isEqualTo("ACTIVA");
    }

    @Test
    void impactoCriticoAplicaMenos40YMas45SinPasarseDeLosLimites() {
        when(storyNodeRepository.findByNodeCode("NODE-ESPEJO")).thenReturn(Optional.of(node));

        CreateDecisionRequest request = new CreateDecisionRequest(10L,
                "Stefan sigue adelante con lo que el guion le indica.", "CRITICO");

        DecisionResponse response = service.decide(request, null);

        assertThat(response.lucidity()).isEqualTo(60);
        assertThat(response.controlLevel()).isEqualTo(45);
    }

    @Test
    void controlLevel100TerminaConEndingPacSymbolAunqueLucidezTambienLlegueACero() {
        playthrough.setLucidity(40);
        playthrough.setControlLevel(60);
        when(storyNodeRepository.findByNodeCode(any())).thenReturn(Optional.of(node));

        CreateDecisionRequest request = new CreateDecisionRequest(10L,
                "Stefan sigue adelante con lo que el guion le indica.", "CRITICO");

        DecisionResponse response = service.decide(request, null);

        assertThat(response.controlLevel()).isEqualTo(100);
        assertThat(response.lucidity()).isEqualTo(0);
        assertThat(response.playthroughStatus()).isEqualTo("FINALIZADA");
        assertThat(response.endingCode()).isEqualTo("ENDING_PAC_SYMBOL");
    }

    @Test
    void publishEventSeLlamaUnaVezEnNormalYCeroVecesEnEntradaCorrupta() {
        when(storyNodeRepository.findByNodeCode(any())).thenReturn(Optional.of(node));

        CreateDecisionRequest normal = new CreateDecisionRequest(10L,
                "Stefan acepta la oferta y sigue el guion previsto.", "LEVE");
        service.decide(normal, null);
        verify(eventPublisher, times(1)).publishEvent(any(Object.class));

        reset(eventPublisher);
        playthrough.setStatus("ACTIVA");
        CreateDecisionRequest corrupta = new CreateDecisionRequest(10L, "%%% 0101 ### @@ 11", "LEVE");
        service.decide(corrupta, null);
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }
}
