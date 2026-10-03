package gt.edu.uinsight.alert.service;

import gt.edu.uinsight.alert.dto.request.AlertFilterRequest;
import gt.edu.uinsight.alert.dto.response.AlertResponse;
import gt.edu.uinsight.alert.dto.response.AlertStatusHistoryResponse;
import gt.edu.uinsight.alert.dto.response.AlertSummaryResponse;
import gt.edu.uinsight.alert.entity.Alert;
import gt.edu.uinsight.alert.entity.AlertSeverity;
import gt.edu.uinsight.alert.entity.AlertStatus;
import gt.edu.uinsight.alert.entity.AlertStatusHistory;
import gt.edu.uinsight.alert.entity.AlertType;
import gt.edu.uinsight.alert.exception.AlertNotFoundException;
import gt.edu.uinsight.alert.exception.InvalidAlertDataException;
import gt.edu.uinsight.alert.exception.InvalidAlertStatusTransitionException;
import gt.edu.uinsight.alert.mapper.AlertMapper;
import gt.edu.uinsight.alert.repository.AlertRepository;
import gt.edu.uinsight.alert.repository.AlertStatusHistoryRepository;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ciclo de vida de las alertas (célula C3): consulta, filtros, cambios de
 * estado con validación de transiciones, historial y conteos.
 *
 * Otros módulos que dependen de C3:
 * - C4 (intervenciones): exists(id) / isActive(id).
 * - C5 (reportes):       getAlerts(filtro) y getSummary().
 * - C6 (frontend):       API REST de AlertController.
 */
@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    private static final Sort NEWEST_FIRST = Sort.by(
            Sort.Order.desc("generatedAt"),
            Sort.Order.desc("id")
    );

    private final AlertRepository alertRepository;
    private final AlertStatusHistoryRepository historyRepository;

    public AlertService(
            AlertRepository alertRepository,
            AlertStatusHistoryRepository historyRepository
    ) {
        this.alertRepository = alertRepository;
        this.historyRepository = historyRepository;
    }

    @Transactional(readOnly = true)
    public List<AlertResponse> getAllAlerts() {
        return getAlerts(AlertFilterRequest.none());
    }

    @Transactional(readOnly = true)
    public List<AlertResponse> getActiveAlerts() {
        return getAlerts(new AlertFilterRequest(null, null, null, null, null, true));
    }

    /**
     * Lista alertas aplicando los filtros recibidos (todos opcionales),
     * de la más reciente a la más antigua.
     */
    @Transactional(readOnly = true)
    public List<AlertResponse> getAlerts(AlertFilterRequest filter) {

        AlertFilterRequest criteria = filter != null ? filter : AlertFilterRequest.none();

        if (criteria.sectionIds() != null && criteria.sectionIds().isEmpty()) {
            return List.of();
        }

        String severity = normalizeSeverity(criteria.severity());
        String alertType = normalizeType(criteria.alertType());

        Specification<Alert> specification = (root, query, builder) -> {

            List<Predicate> predicates = new ArrayList<>();

            if (criteria.status() != null) {
                predicates.add(builder.equal(root.get("status"), criteria.status()));
            }

            if (Boolean.TRUE.equals(criteria.activeOnly())) {
                predicates.add(root.get("status").in(AlertStatus.ACTIVE_STATUSES));
            }

            if (severity != null) {
                predicates.add(builder.equal(root.get("severity"), severity));
            }

            if (alertType != null) {
                predicates.add(builder.equal(root.get("alertType"), alertType));
            }

            if (criteria.sectionId() != null) {
                predicates.add(builder.equal(root.get("sectionId"), criteria.sectionId()));
            }

            if (criteria.sectionIds() != null) {
                predicates.add(root.get("sectionId").in(criteria.sectionIds()));
            }

            return builder.and(predicates.toArray(new Predicate[0]));
        };

        return alertRepository.findAll(specification, NEWEST_FIRST)
                .stream()
                .map(AlertMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AlertResponse getAlertById(Long id) {
        return AlertMapper.toResponse(findAlert(id));
    }

    @Transactional(readOnly = true)
    public List<AlertStatusHistoryResponse> getHistory(Long id) {

        findAlert(id);

        return historyRepository.findByAlertIdOrderByChangedAtAscIdAsc(id)
                .stream()
                .map(AlertMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AlertSummaryResponse getSummary() {

        Map<String, Long> byStatus = new LinkedHashMap<>();
        long total = 0;

        for (AlertStatus status : AlertStatus.values()) {
            long count = alertRepository.countByStatus(status);
            byStatus.put(status.name(), count);
            total += count;
        }

        Map<String, Long> activeBySeverity = new LinkedHashMap<>();
        long active = 0;

        for (AlertSeverity severity : AlertSeverity.values()) {
            long count = alertRepository.countByStatusInAndSeverity(
                    AlertStatus.ACTIVE_STATUSES,
                    severity.name()
            );
            activeBySeverity.put(severity.name(), count);
            active += count;
        }

        return new AlertSummaryResponse(total, active, byStatus, activeBySeverity);
    }

    @Transactional
    public AlertResponse updateStatus(Long id, AlertStatus requestedStatus) {
        return updateStatus(id, requestedStatus, null);
    }

    @Transactional
    public AlertResponse updateStatus(
            Long id,
            AlertStatus requestedStatus,
            String comment
    ) {

        if (requestedStatus == null) {
            throw new InvalidAlertDataException("status es obligatorio");
        }

        Alert alert = findAlert(id);

        AlertStatus currentStatus = alert.getStatus();

        validateTransition(id, currentStatus, requestedStatus);

        alert.setStatus(requestedStatus);

        if (requestedStatus.isFinal()) {
            alert.setResolvedAt(LocalDateTime.now());
        }

        Alert updatedAlert = alertRepository.save(alert);

        historyRepository.save(
                new AlertStatusHistory(
                        id,
                        currentStatus,
                        requestedStatus,
                        normalizeComment(comment)
                )
        );

        log.info(
                "ALERT_UPDATED module=alert alertId={} from={} to={}",
                id, currentStatus, requestedStatus
        );

        return AlertMapper.toResponse(updatedAlert);
    }

    /** Lo usa C4 para validar que la alerta existe (RN-1 de intervenciones). */
    @Transactional(readOnly = true)
    public boolean exists(Long id) {
        return id != null && alertRepository.existsById(id);
    }

    /**
     * Lo usa C4 para validar que la alerta sigue abierta (RN-2 de
     * intervenciones). Lanza AlertNotFoundException si no existe.
     */
    @Transactional(readOnly = true)
    public boolean isActive(Long id) {
        return findAlert(id).getStatus().isActive();
    }

    private Alert findAlert(Long id) {

        return alertRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("ALERT_NOT_FOUND module=alert alertId={}", id);
                    return new AlertNotFoundException(id);
                });
    }

    private void validateTransition(
            Long id,
            AlertStatus current,
            AlertStatus requested
    ) {

        if (!current.canTransitionTo(requested)) {

            log.warn(
                    "INVALID_ALERT_TRANSITION module=alert alertId={} from={} to={}",
                    id, current, requested
            );

            throw new InvalidAlertStatusTransitionException(current, requested);
        }
    }

    private String normalizeSeverity(String severity) {

        if (severity == null || severity.isBlank()) {
            return null;
        }

        return AlertSeverity.parse(severity)
                .orElseThrow(() -> new InvalidAlertDataException(
                        "severity inválida: " + severity + ". Valores: LOW, MEDIUM, HIGH"))
                .name();
    }

    private String normalizeType(String alertType) {

        if (alertType == null || alertType.isBlank()) {
            return null;
        }

        return AlertType.parse(alertType)
                .orElseThrow(() -> new InvalidAlertDataException(
                        "alertType inválido: " + alertType + ". Valores: PERFORMANCE, TREND, DISPERSION"))
                .name();
    }

    private String normalizeComment(String comment) {

        if (comment == null || comment.isBlank()) {
            return null;
        }

        return comment.trim();
    }
}
