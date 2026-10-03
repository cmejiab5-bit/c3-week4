package gt.edu.uinsight.alert.exception;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Formato de error común del proyecto (sección 10.1 del documento):
 * { timestamp, status, error, message, details, traceId }.
 */
public record AlertErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        List<String> details,
        String traceId
) {
}
