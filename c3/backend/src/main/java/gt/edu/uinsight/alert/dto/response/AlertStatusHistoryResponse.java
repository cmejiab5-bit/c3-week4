package gt.edu.uinsight.alert.dto.response;

import gt.edu.uinsight.alert.entity.AlertStatus;

import java.time.LocalDateTime;

public record AlertStatusHistoryResponse(

        Long id,
        Long alertId,
        AlertStatus fromStatus,
        AlertStatus toStatus,
        String comment,
        LocalDateTime changedAt

) {
}
