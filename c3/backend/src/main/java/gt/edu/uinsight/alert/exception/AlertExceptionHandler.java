package gt.edu.uinsight.alert.exception;

import gt.edu.uinsight.alert.controller.AlertController;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Manejador de errores de C3. Está acotado a {@link AlertController} y tiene
 * máxima prioridad para que ningún manejador global de otra célula cambie el
 * formato de error de las alertas.
 */
@RestControllerAdvice(assignableTypes = AlertController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AlertExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(AlertExceptionHandler.class);

    @ExceptionHandler(AlertNotFoundException.class)
    public ResponseEntity<AlertErrorResponse> handleNotFound(
            AlertNotFoundException ex,
            HttpServletRequest request
    ) {
        return build(HttpStatus.NOT_FOUND, "ALERT_NOT_FOUND", ex.getMessage(), List.of(), request);
    }

    @ExceptionHandler(InvalidAlertStatusTransitionException.class)
    public ResponseEntity<AlertErrorResponse> handleInvalidTransition(
            InvalidAlertStatusTransitionException ex,
            HttpServletRequest request
    ) {
        return build(HttpStatus.CONFLICT, "INVALID_STATUS_TRANSITION", ex.getMessage(), List.of(), request);
    }

    @ExceptionHandler(InvalidAlertDataException.class)
    public ResponseEntity<AlertErrorResponse> handleInvalidData(
            InvalidAlertDataException ex,
            HttpServletRequest request
    ) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_ALERT_DATA", ex.getMessage(), List.of(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<AlertErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .toList();

        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Validation failed", details, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<AlertErrorResponse> handleUnreadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        return build(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "El cuerpo de la petición no es válido",
                List.of("Verifique el JSON y que status sea uno de: NEW, UNDER_REVIEW, IN_PROGRESS, RESOLVED, DISMISSED"),
                request
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<AlertErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {
        return build(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "Parámetro inválido",
                List.of("El valor de '" + ex.getName() + "' no es válido"),
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<AlertErrorResponse> handleUnexpected(
            Exception ex,
            HttpServletRequest request
    ) {
        String traceId = resolveTraceId(request);
        log.error("UNEXPECTED_ERROR module=alert traceId={}", traceId, ex);

        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "UNEXPECTED_ERROR",
                "Ocurrió un error inesperado",
                List.of(),
                request,
                traceId
        );
    }

    private String formatFieldError(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }

    private ResponseEntity<AlertErrorResponse> build(
            HttpStatus status,
            String error,
            String message,
            List<String> details,
            HttpServletRequest request
    ) {
        return build(status, error, message, details, request, resolveTraceId(request));
    }

    private ResponseEntity<AlertErrorResponse> build(
            HttpStatus status,
            String error,
            String message,
            List<String> details,
            HttpServletRequest request,
            String traceId
    ) {
        if (status.is4xxClientError()) {
            log.warn("{} module=alert path={} traceId={} message={}",
                    error, request.getRequestURI(), traceId, message);
        }

        AlertErrorResponse body = new AlertErrorResponse(
                LocalDateTime.now(),
                status.value(),
                error,
                message,
                details,
                traceId
        );

        return ResponseEntity.status(status).body(body);
    }

    /**
     * Usa el traceId que ya exista (MDC de C7 o cabeceras X-Trace-Id /
     * X-Correlation-ID que reenvía NGINX); si no hay, genera uno.
     */
    private String resolveTraceId(HttpServletRequest request) {

        String traceId = MDC.get("traceId");

        if (isBlank(traceId)) {
            traceId = request.getHeader("X-Trace-Id");
        }

        if (isBlank(traceId)) {
            traceId = request.getHeader("X-Correlation-ID");
        }

        if (isBlank(traceId)) {
            traceId = "REQ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }

        return traceId;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
