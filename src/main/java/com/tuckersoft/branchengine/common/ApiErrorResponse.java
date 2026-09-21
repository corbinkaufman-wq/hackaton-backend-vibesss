package com.tuckersoft.branchengine.common;

import java.time.Instant;

/**
 * Formato de error unico del enunciado: { error, message, timestamp, path }.
 * Se usa tanto en el @RestControllerAdvice como en el AuthenticationEntryPoint
 * y el AccessDeniedHandler de seguridad (estrella Seguridad), para que los 401
 * y 403 tengan exactamente el mismo formato que el resto de errores.
 */
public record ApiErrorResponse(String error, String message, Instant timestamp, String path) {
}
