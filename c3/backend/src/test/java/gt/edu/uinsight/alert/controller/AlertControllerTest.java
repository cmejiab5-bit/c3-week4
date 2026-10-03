package gt.edu.uinsight.alert.controller;

import gt.edu.uinsight.alert.dto.response.AlertResponse;
import gt.edu.uinsight.alert.entity.AlertStatus;
import gt.edu.uinsight.alert.exception.AlertExceptionHandler;
import gt.edu.uinsight.alert.exception.AlertNotFoundException;
import gt.edu.uinsight.alert.exception.InvalidAlertStatusTransitionException;
import gt.edu.uinsight.alert.service.AlertService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba la capa HTTP de C3 (rutas, códigos y formato de error común)
 * sin levantar Spring completo.
 */
@ExtendWith(MockitoExtension.class)
class AlertControllerTest {

    @Mock
    private AlertService alertService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AlertController(alertService))
                .setControllerAdvice(new AlertExceptionHandler())
                .build();
    }

    private AlertResponse sampleAlert(AlertStatus status) {
        return new AlertResponse(
                10L, 1L, "TREND", "HIGH", "Tendencia negativa", "desc",
                status, status.isActive(), status.allowedTransitions(),
                LocalDateTime.of(2026, 9, 20, 10, 0), null
        );
    }

    @Test
    void getAlertById_responde200ConLaAlerta() throws Exception {
        when(alertService.getAlertById(10L)).thenReturn(sampleAlert(AlertStatus.NEW));

        mockMvc.perform(get("/api/v1/alerts/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.allowedTransitions[0]").value("UNDER_REVIEW"));
    }

    @Test
    void getAlertById_responde404ConFormatoDeErrorComun() throws Exception {
        when(alertService.getAlertById(99L)).thenThrow(new AlertNotFoundException(99L));

        mockMvc.perform(get("/api/v1/alerts/99").header("X-Trace-Id", "REQ-TEST"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("ALERT_NOT_FOUND"))
                .andExpect(jsonPath("$.traceId").value("REQ-TEST"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void getAlerts_conIdNoNumericoResponde400() throws Exception {
        mockMvc.perform(get("/api/v1/alerts/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void getAlerts_conEstadoInvalidoResponde400() throws Exception {
        mockMvc.perform(get("/api/v1/alerts").param("status", "CLOSED"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void getAlerts_devuelveLaLista() throws Exception {
        when(alertService.getAlerts(any())).thenReturn(List.of(sampleAlert(AlertStatus.NEW)));

        mockMvc.perform(get("/api/v1/alerts").param("severity", "HIGH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].severity").value("HIGH"));
    }

    @Test
    void updateStatus_responde200CuandoLaTransicionEsValida() throws Exception {
        when(alertService.updateStatus(eq(10L), eq(AlertStatus.UNDER_REVIEW), any()))
                .thenReturn(sampleAlert(AlertStatus.UNDER_REVIEW));

        mockMvc.perform(patch("/api/v1/alerts/10/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"UNDER_REVIEW\",\"comment\":\"ok\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNDER_REVIEW"));
    }

    @Test
    void updateStatus_responde409CuandoLaTransicionNoEstaPermitida() throws Exception {
        when(alertService.updateStatus(eq(10L), eq(AlertStatus.RESOLVED), any()))
                .thenThrow(new InvalidAlertStatusTransitionException(
                        AlertStatus.NEW, AlertStatus.RESOLVED));

        mockMvc.perform(patch("/api/v1/alerts/10/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"RESOLVED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("INVALID_STATUS_TRANSITION"));
    }

    @Test
    void updateStatus_responde400SiFaltaElStatus() throws Exception {
        mockMvc.perform(patch("/api/v1/alerts/10/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[0]").value("status: status es obligatorio"));
    }

    @Test
    void updateStatus_responde400ConUnEstadoQueNoExiste() throws Exception {
        mockMvc.perform(patch("/api/v1/alerts/10/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CLOSED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }
}
