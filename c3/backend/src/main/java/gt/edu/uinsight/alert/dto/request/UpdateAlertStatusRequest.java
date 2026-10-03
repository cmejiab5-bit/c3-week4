package gt.edu.uinsight.alert.dto.request;

import gt.edu.uinsight.alert.entity.AlertStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateAlertStatusRequest(

        @Schema(example = "UNDER_REVIEW")
        @NotNull(message = "status es obligatorio")
        AlertStatus status,

        @Schema(
                description = "Comentario opcional que queda en el historial de la alerta",
                example = "Se revisará con el docente de la sección"
        )
        @Size(max = 255, message = "comment no puede superar 255 caracteres")
        String comment

) {
}
