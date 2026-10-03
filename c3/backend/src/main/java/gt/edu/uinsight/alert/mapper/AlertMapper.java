package gt.edu.uinsight.alert.mapper;

import gt.edu.uinsight.alert.dto.response.AlertResponse;
import gt.edu.uinsight.alert.dto.response.AlertStatusHistoryResponse;
import gt.edu.uinsight.alert.entity.Alert;
import gt.edu.uinsight.alert.entity.AlertStatusHistory;

public final class AlertMapper {

    private AlertMapper() {
    }

    public static AlertResponse toResponse(Alert alert) {

        return new AlertResponse(
                alert.getId(),
                alert.getSectionId(),
                alert.getAlertType(),
                alert.getSeverity(),
                alert.getTitle(),
                alert.getDescription(),
                alert.getStatus(),
                alert.getStatus().isActive(),
                alert.getStatus().allowedTransitions(),
                alert.getGeneratedAt(),
                alert.getResolvedAt()
        );
    }

    public static AlertStatusHistoryResponse toResponse(AlertStatusHistory history) {

        return new AlertStatusHistoryResponse(
                history.getId(),
                history.getAlertId(),
                history.getFromStatus(),
                history.getToStatus(),
                history.getComment(),
                history.getChangedAt()
        );
    }
}
