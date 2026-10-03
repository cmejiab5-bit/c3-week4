package gt.edu.uinsight.alert.dto.response;

import gt.edu.uinsight.alert.entity.AlertStatus;

import java.time.LocalDateTime;
import java.util.List;

public record AlertResponse(

        Long id,
        Long sectionId,
        String alertType,
        String severity,
        String title,
        String description,
        AlertStatus status,
        boolean active,
        List<AlertStatus> allowedTransitions,
        LocalDateTime generatedAt,
        LocalDateTime resolvedAt

) {
}
