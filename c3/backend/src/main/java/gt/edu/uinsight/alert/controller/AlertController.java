package gt.edu.uinsight.alert.controller;

import gt.edu.uinsight.alert.dto.request.AlertFilterRequest;
import gt.edu.uinsight.alert.dto.request.UpdateAlertStatusRequest;
import gt.edu.uinsight.alert.dto.response.AlertResponse;
import gt.edu.uinsight.alert.dto.response.AlertStatusHistoryResponse;
import gt.edu.uinsight.alert.dto.response.AlertSummaryResponse;
import gt.edu.uinsight.alert.entity.AlertStatus;
import gt.edu.uinsight.alert.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/alerts")
@Tag(
        name = "C3 - Alertas",
        description = "Gestión del ciclo de vida de las alertas"
)
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @Operation(
            summary = "Consultar alertas",
            description = "Obtiene las alertas, de la más reciente a la más antigua. "
                    + "Todos los filtros son opcionales y se pueden combinar."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado obtenido"),
            @ApiResponse(responseCode = "400", description = "Filtro inválido")
    })
    @GetMapping
    public ResponseEntity<List<AlertResponse>> getAlerts(
            @Parameter(description = "Estado: NEW, UNDER_REVIEW, IN_PROGRESS, RESOLVED, DISMISSED")
            @RequestParam(required = false) AlertStatus status,

            @Parameter(description = "Severidad: LOW, MEDIUM, HIGH", example = "HIGH")
            @RequestParam(required = false) String severity,

            @Parameter(description = "Tipo: PERFORMANCE, TREND, DISPERSION", example = "TREND")
            @RequestParam(required = false) String alertType,

            @Parameter(description = "Id de la sección", example = "10")
            @RequestParam(required = false) Long sectionId
    ) {

        AlertFilterRequest filter = new AlertFilterRequest(
                status,
                severity,
                alertType,
                sectionId,
                null,
                null
        );

        return ResponseEntity.ok(alertService.getAlerts(filter));
    }

    @Operation(
            summary = "Consultar alertas activas",
            description = "Obtiene las alertas que todavía se encuentran abiertas "
                    + "(NEW, UNDER_REVIEW o IN_PROGRESS)."
    )
    @GetMapping("/active")
    public ResponseEntity<List<AlertResponse>> getActiveAlerts() {

        return ResponseEntity.ok(
                alertService.getActiveAlerts()
        );
    }

    @Operation(
            summary = "Resumen de alertas",
            description = "Conteo de alertas por estado y de alertas activas por severidad."
    )
    @GetMapping("/summary")
    public ResponseEntity<AlertSummaryResponse> getSummary() {

        return ResponseEntity.ok(
                alertService.getSummary()
        );
    }

    @Operation(
            summary = "Consultar alerta por ID",
            description = "Obtiene una alerta específica, incluyendo los estados a los que puede pasar."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Alerta encontrada"),
            @ApiResponse(responseCode = "404", description = "La alerta no existe")
    })
    @GetMapping("/{id}")
    public ResponseEntity<AlertResponse> getAlertById(
            @Parameter(description = "Id de la alerta", example = "10")
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                alertService.getAlertById(id)
        );
    }

    @Operation(
            summary = "Historial de estados de una alerta",
            description = "Lista los cambios de estado de la alerta, del más antiguo al más reciente."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historial obtenido"),
            @ApiResponse(responseCode = "404", description = "La alerta no existe")
    })
    @GetMapping("/{id}/history")
    public ResponseEntity<List<AlertStatusHistoryResponse>> getHistory(
            @Parameter(description = "Id de la alerta", example = "10")
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                alertService.getHistory(id)
        );
    }

    @Operation(
            summary = "Actualizar estado de una alerta",
            description = "Actualiza el estado respetando las transiciones permitidas del ciclo de vida."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado actualizado"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "La alerta no existe"),
            @ApiResponse(responseCode = "409", description = "Transición de estado no permitida")
    })
    @PatchMapping("/{id}/status")
    public ResponseEntity<AlertResponse> updateStatus(
            @Parameter(description = "Id de la alerta", example = "10")
            @PathVariable Long id,
            @Valid @RequestBody UpdateAlertStatusRequest request
    ) {

        return ResponseEntity.ok(
                alertService.updateStatus(
                        id,
                        request.status(),
                        request.comment()
                )
        );
    }
}
