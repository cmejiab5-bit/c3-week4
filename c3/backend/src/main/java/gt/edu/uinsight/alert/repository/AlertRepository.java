package gt.edu.uinsight.alert.repository;

import gt.edu.uinsight.alert.entity.Alert;
import gt.edu.uinsight.alert.entity.AlertStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AlertRepository
        extends JpaRepository<Alert, Long>, JpaSpecificationExecutor<Alert> {

    List<Alert> findByStatusIn(List<AlertStatus> statuses);

    List<Alert> findBySectionId(Long sectionId);

    long countByStatus(AlertStatus status);

    long countByStatusInAndSeverity(Collection<AlertStatus> statuses, String severity);

    /**
     * Busca una alerta abierta del mismo tipo para la misma sección.
     * Lo usa AlertGenerationService para que B7 no genere duplicados.
     */
    Optional<Alert> findFirstBySectionIdAndAlertTypeAndStatusIn(
            Long sectionId,
            String alertType,
            Collection<AlertStatus> statuses
    );
}
