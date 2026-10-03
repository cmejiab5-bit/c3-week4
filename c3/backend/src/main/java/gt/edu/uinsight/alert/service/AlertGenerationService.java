package gt.edu.uinsight.alert.service;

import gt.edu.uinsight.alert.dto.request.GenerateAlertRequest;
import gt.edu.uinsight.alert.dto.response.AlertGenerationResult;
import gt.edu.uinsight.alert.entity.Alert;
import gt.edu.uinsight.alert.entity.AlertSeverity;
import gt.edu.uinsight.alert.entity.AlertStatus;
import gt.edu.uinsight.alert.entity.AlertStatusHistory;
import gt.edu.uinsight.alert.entity.AlertType;
import gt.edu.uinsight.alert.exception.InvalidAlertDataException;
import gt.edu.uinsight.alert.mapper.AlertMapper;
import gt.edu.uinsight.alert.repository.AlertRepository;
import gt.edu.uinsight.alert.repository.AlertStatusHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Punto de entrada para que B7 (motor de detección de riesgos) escriba
 * alertas. Centraliza dos reglas de C3:
 *
 * 1. Datos válidos (tipo, severidad, título).
 * 2. Sin duplicados: si la sección ya tiene una alerta abierta del mismo
 *    tipo, no se crea otra y se devuelve la existente.
 */
@Service
public class AlertGenerationService {

    private static final Logger log = LoggerFactory.getLogger(AlertGenerationService.class);

    private static final int TITLE_MAX_LENGTH = 150;
    private static final int DESCRIPTION_MAX_LENGTH = 500;

    private final AlertRepository alertRepository;
    private final AlertStatusHistoryRepository historyRepository;

    public AlertGenerationService(
            AlertRepository alertRepository,
            AlertStatusHistoryRepository historyRepository
    ) {
        this.alertRepository = alertRepository;
        this.historyRepository = historyRepository;
    }

    @Transactional
    public AlertGenerationResult generate(GenerateAlertRequest request) {

        if (request == null) {
            throw new InvalidAlertDataException("La solicitud de alerta es obligatoria");
        }

        Long sectionId = requireSection(request.sectionId());
        AlertType type = requireType(request.alertType());
        AlertSeverity severity = requireSeverity(request.severity());
        String title = requireTitle(request.title());
        String description = normalizeDescription(request.description());

        Optional<Alert> existing = alertRepository
                .findFirstBySectionIdAndAlertTypeAndStatusIn(
                        sectionId,
                        type.name(),
                        AlertStatus.ACTIVE_STATUSES
                );

        if (existing.isPresent()) {

            log.warn(
                    "ALERT_ALREADY_EXISTS module=alert alertId={} sectionId={} alertType={}",
                    existing.get().getId(), sectionId, type
            );

            return new AlertGenerationResult(false, AlertMapper.toResponse(existing.get()));
        }

        Alert saved = alertRepository.save(
                new Alert(sectionId, type.name(), severity.name(), title, description)
        );

        historyRepository.save(
                new AlertStatusHistory(saved.getId(), null, AlertStatus.NEW, "Alerta generada")
        );

        log.info(
                "ALERT_CREATED module=alert alertId={} sectionId={} alertType={} severity={}",
                saved.getId(), sectionId, type, severity
        );

        return new AlertGenerationResult(true, AlertMapper.toResponse(saved));
    }

    private Long requireSection(Long sectionId) {
        if (sectionId == null) {
            throw new InvalidAlertDataException("sectionId es obligatorio");
        }
        return sectionId;
    }

    private AlertType requireType(String alertType) {
        return AlertType.parse(alertType)
                .orElseThrow(() -> new InvalidAlertDataException(
                        "alertType inválido: " + alertType + ". Valores: PERFORMANCE, TREND, DISPERSION"));
    }

    private AlertSeverity requireSeverity(String severity) {
        return AlertSeverity.parse(severity)
                .orElseThrow(() -> new InvalidAlertDataException(
                        "severity inválida: " + severity + ". Valores: LOW, MEDIUM, HIGH"));
    }

    private String requireTitle(String title) {

        if (title == null || title.isBlank()) {
            throw new InvalidAlertDataException("title es obligatorio");
        }

        String trimmed = title.trim();

        if (trimmed.length() > TITLE_MAX_LENGTH) {
            throw new InvalidAlertDataException(
                    "title no puede superar " + TITLE_MAX_LENGTH + " caracteres");
        }

        return trimmed;
    }

    private String normalizeDescription(String description) {

        if (description == null || description.isBlank()) {
            return null;
        }

        String trimmed = description.trim();

        if (trimmed.length() > DESCRIPTION_MAX_LENGTH) {
            throw new InvalidAlertDataException(
                    "description no puede superar " + DESCRIPTION_MAX_LENGTH + " caracteres");
        }

        return trimmed;
    }
}
