package com.tuckersoft.branchengine.decision;

import com.tuckersoft.branchengine.decision.event.DecisionCommittedEvent;
import com.tuckersoft.branchengine.entity.Decision;
import com.tuckersoft.branchengine.entity.RealityLog;
import com.tuckersoft.branchengine.repository.DecisionRepository;
import com.tuckersoft.branchengine.repository.RealityLogRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;

/**
 * Componente separado de DecisionService a proposito: el listener corre en
 * otro hilo (branch-worker-N) DESPUES del commit de la transaccion original,
 * y necesita su propia transaccion (REQUIRES_NEW) porque Spring rechaza un
 * @Transactional normal sobre un @TransactionalEventListener.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BranchNotificationListener {

    private final DecisionRepository decisionRepository;
    private final RealityLogRepository realityLogRepository;
    private final JavaMailSender mailSender;

    @Async("branchExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alCommit(DecisionCommittedEvent event) {
        Decision decision = decisionRepository.findById(event.decisionId()).orElse(null);
        if (decision == null) {
            return;
        }

        decision.setStatus("PROCESANDO");
        decisionRepository.save(decision);

        String subject = "[TUCKERSOFT] " + event.branchType() + " en " + event.playerTag()
                + " | Impacto " + event.impactLevel();

        RealityLog logEntry = new RealityLog();
        logEntry.setDecision(decision);
        logEntry.setRecipientEmail(event.recipientEmail());
        logEntry.setSubject(subject);
        logEntry.setCreatedAt(Instant.now());

        try {
            if (event.simulateMailFailure()) {
                throw new IllegalStateException(
                        "Fallo simulado de correo (X-Bandersnatch-Simulate: MAIL_FAILURE)");
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setTo(event.recipientEmail());
            helper.setSubject(subject);
            helper.setText(buildBody(event));
            mailSender.send(message);

            logEntry.setLogStatus("SENT");
            logEntry.setSentAt(Instant.now());
            decision.setStatus("ESTABILIZADA");
        } catch (Exception ex) {
            logEntry.setLogStatus("FAILED");
            logEntry.setErrorMessage(ex.getMessage());
            decision.setStatus("ERROR");
            log.error("Fallo al enviar el Informe de Realidad de la decision {}", event.decisionId(), ex);
        }

        realityLogRepository.save(logEntry);
        decisionRepository.save(decision);

        log.info("[BRANCH-LOG] Decision ID: {} | Player: {} | Branch: {} | Impact: {} | Unit: {} | "
                        + "Node: {} -> {} | Thread: {} | Status: {}",
                decision.getId(), event.playerTag(), event.branchType(), event.impactLevel(), event.handlerUnit(),
                event.sourceNodeCode(), event.resolvedNodeCode(), Thread.currentThread().getName(),
                decision.getStatus());
    }

    private String buildBody(DecisionCommittedEvent event) {
        String resolvedNode = event.resolvedNodeCode() == null ? "-" : event.resolvedNodeCode();
        String ending = event.endingCode() == null ? "-" : event.endingCode();

        return """
                Hola %s,

                Una partida de prueba acaba de ramificarse.

                ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                Decision ID      : #%d
                Jugador          : %s
                Rama             : %s
                Impacto          : %s
                Departamento     : %s
                Consecuencia     : %s
                Nodo origen      : %s
                Nodo destino     : %s
                Estado partida   : %s
                Lucidez          : %d/100
                Nivel de control : %d/100
                Final            : %s
                Registrada       : %s
                ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

                Decisión original del jugador:
                "%s"

                — Tuckersoft Branch Engine, 1984
                """.formatted(
                event.recipientDisplayName(),
                event.decisionId(),
                event.playerTag(),
                event.branchType(),
                event.impactLevel(),
                event.handlerUnit(),
                event.outcomeCode(),
                event.sourceNodeCode(),
                resolvedNode,
                event.playthroughStatus(),
                event.lucidity(),
                event.controlLevel(),
                ending,
                event.createdAt(),
                event.rawInput()
        );
    }
}
