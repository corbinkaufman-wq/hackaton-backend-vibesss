# ENTREGA — Tuckersoft Branch Engine

**Equipo:** VIBESSS
**Integrantes:** Walter Aquino, Corbin Kaufman

## Resultado de los autotests

```
★★★★★   5 / 5   Cinco estrellas.

 ✔  ★1  SEGURIDAD    65 comprobaciones
 ✔  ★2  NODOS        37 comprobaciones
 ✔  ★3  PARTIDAS     40 comprobaciones
 ✔  ★4  DECISIONES   101 comprobaciones
 ✔  ★5  ASINCRONIA   41 comprobaciones

Tests run: 52, Failures: 0, Errors: 0, Skipped: 0
```

## Flujo asíncrono

`POST /api/v1/decisions` responde `201` de inmediato, sin esperar al envío del
correo. Dentro de la misma transacción, `DecisionService` clasifica la
decisión, actualiza los stats de la partida y publica un
`DecisionCommittedEvent` vía `ApplicationEventPublisher`.

`BranchNotificationListener` es un `@Component` aparte (nunca inyectado en
`DecisionService`) que escucha ese evento con
`@TransactionalEventListener(phase = AFTER_COMMIT)`, así solo se dispara
**después** de que PostgreSQL confirma el commit — evitando que el listener
busque en la base de datos una decisión que todavía no existe ahí. Corre en
un hilo separado (`@Async("branchExecutor")`, un `ThreadPoolTaskExecutor`
dedicado con prefijo `branch-worker-`) y con su propia transacción
(`@Transactional(propagation = REQUIRES_NEW)`, obligatorio porque Spring
rechaza un `@Transactional` normal sobre un `@TransactionalEventListener`).

El listener marca la decisión como `PROCESANDO`, intenta enviar el Informe de
Realidad con `JavaMailSender`, y según el resultado deja la decisión en
`ESTABILIZADA` (+ `RealityLog` con `logStatus=SENT`) o en `ERROR` (+
`RealityLog` con `logStatus=FAILED` y el mensaje de la excepción). La cabecera
`X-Bandersnatch-Simulate: MAIL_FAILURE` fuerza ese segundo camino para poder
probarlo sin depender de un fallo real de SMTP.

## Lo que no llegamos a terminar

Nada — las 5 estrellas están completas y verificadas contra la batería de
autotests del TA.
