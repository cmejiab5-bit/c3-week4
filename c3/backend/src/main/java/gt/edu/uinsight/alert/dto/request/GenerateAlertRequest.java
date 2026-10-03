package gt.edu.uinsight.alert.dto.request;

/**
 * Datos que B7 (motor de riesgos) entrega a C3 para generar una alerta.
 * No es un endpoint HTTP: se usa llamando a AlertGenerationService.
 *
 * alertType: PERFORMANCE | TREND | DISPERSION
 * severity:  LOW | MEDIUM | HIGH
 */
public record GenerateAlertRequest(
        Long sectionId,
        String alertType,
        String severity,
        String title,
        String description
) {
}
