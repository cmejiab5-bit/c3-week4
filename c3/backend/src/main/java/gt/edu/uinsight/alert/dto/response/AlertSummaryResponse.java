package gt.edu.uinsight.alert.dto.response;

import java.util.Map;

/**
 * Conteos de alertas para el inicio del frontend (C6) y los reportes (C5).
 */
public record AlertSummaryResponse(

        long total,
        long active,
        Map<String, Long> byStatus,
        Map<String, Long> activeBySeverity

) {
}
