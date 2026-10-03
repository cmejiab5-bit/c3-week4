package gt.edu.uinsight.alert.service;

import gt.edu.uinsight.alert.dto.request.AlertFilterRequest;
import gt.edu.uinsight.alert.dto.response.AlertResponse;
import gt.edu.uinsight.alert.dto.response.AlertSummaryResponse;
import gt.edu.uinsight.alert.entity.Alert;
import gt.edu.uinsight.alert.entity.AlertStatus;
import gt.edu.uinsight.alert.entity.AlertStatusHistory;
import gt.edu.uinsight.alert.exception.AlertNotFoundException;
import gt.edu.uinsight.alert.exception.InvalidAlertDataException;
import gt.edu.uinsight.alert.exception.InvalidAlertStatusTransitionException;
import gt.edu.uinsight.alert.repository.AlertRepository;
import gt.edu.uinsight.alert.repository.AlertStatusHistoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private AlertStatusHistoryRepository historyRepository;

    @InjectMocks
    private AlertService alertService;

    private Alert alertWithStatus(AlertStatus status) {
        Alert alert = new Alert(1L, "TREND", "HIGH", "Tendencia negativa", "desc");
        alert.setStatus(status);
        return alert;
    }

    @ParameterizedTest
    @CsvSource({
            "NEW,UNDER_REVIEW",
            "NEW,DISMISSED",
            "UNDER_REVIEW,IN_PROGRESS",
            "UNDER_REVIEW,DISMISSED",
            "IN_PROGRESS,RESOLVED"
    })
    void updateStatus_permiteLasTransicionesValidas(AlertStatus from, AlertStatus to) {
        when(alertRepository.findById(10L)).thenReturn(Optional.of(alertWithStatus(from)));
        when(alertRepository.save(any(Alert.class))).thenAnswer(i -> i.getArgument(0));

        AlertResponse response = alertService.updateStatus(10L, to);

        assertThat(response.status()).isEqualTo(to);
    }

    @ParameterizedTest
    @CsvSource({
            "NEW,RESOLVED",
            "NEW,IN_PROGRESS",
            "NEW,NEW",
            "UNDER_REVIEW,RESOLVED",
            "IN_PROGRESS,DISMISSED",
            "RESOLVED,IN_PROGRESS",
            "DISMISSED,NEW"
    })
    void updateStatus_rechazaTransicionesInvalidas(AlertStatus from, AlertStatus to) {
        when(alertRepository.findById(10L)).thenReturn(Optional.of(alertWithStatus(from)));

        assertThatThrownBy(() -> alertService.updateStatus(10L, to))
                .isInstanceOf(InvalidAlertStatusTransitionException.class);

        verify(alertRepository, never()).save(any(Alert.class));
        verify(historyRepository, never()).save(any(AlertStatusHistory.class));
    }

    @Test
    void updateStatus_asignaResolvedAtAlResolver() {
        when(alertRepository.findById(10L))
                .thenReturn(Optional.of(alertWithStatus(AlertStatus.IN_PROGRESS)));
        when(alertRepository.save(any(Alert.class))).thenAnswer(i -> i.getArgument(0));

        AlertResponse response = alertService.updateStatus(10L, AlertStatus.RESOLVED);

        assertThat(response.resolvedAt()).isNotNull();
        assertThat(response.active()).isFalse();
    }

    @Test
    void updateStatus_noAsignaResolvedAtEnEstadosIntermedios() {
        when(alertRepository.findById(10L))
                .thenReturn(Optional.of(alertWithStatus(AlertStatus.NEW)));
        when(alertRepository.save(any(Alert.class))).thenAnswer(i -> i.getArgument(0));

        AlertResponse response = alertService.updateStatus(10L, AlertStatus.UNDER_REVIEW);

        assertThat(response.resolvedAt()).isNull();
        assertThat(response.active()).isTrue();
    }

    @Test
    void updateStatus_registraHistorialConComentarioLimpio() {
        when(alertRepository.findById(10L))
                .thenReturn(Optional.of(alertWithStatus(AlertStatus.NEW)));
        when(alertRepository.save(any(Alert.class))).thenAnswer(i -> i.getArgument(0));

        alertService.updateStatus(10L, AlertStatus.UNDER_REVIEW, "  revisar con docente  ");

        ArgumentCaptor<AlertStatusHistory> captor = ArgumentCaptor.forClass(AlertStatusHistory.class);
        verify(historyRepository).save(captor.capture());

        assertThat(captor.getValue().getFromStatus()).isEqualTo(AlertStatus.NEW);
        assertThat(captor.getValue().getToStatus()).isEqualTo(AlertStatus.UNDER_REVIEW);
        assertThat(captor.getValue().getComment()).isEqualTo("revisar con docente");
    }

    @Test
    void updateStatus_lanzaNotFoundSiLaAlertaNoExiste() {
        when(alertRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> alertService.updateStatus(99L, AlertStatus.UNDER_REVIEW))
                .isInstanceOf(AlertNotFoundException.class);
    }

    @Test
    void updateStatus_exigeStatus() {
        assertThatThrownBy(() -> alertService.updateStatus(10L, null))
                .isInstanceOf(InvalidAlertDataException.class);
    }

    @Test
    void getAlertById_incluyeTransicionesPermitidas() {
        when(alertRepository.findById(10L))
                .thenReturn(Optional.of(alertWithStatus(AlertStatus.UNDER_REVIEW)));

        AlertResponse response = alertService.getAlertById(10L);

        assertThat(response.allowedTransitions())
                .containsExactly(AlertStatus.IN_PROGRESS, AlertStatus.DISMISSED);
    }

    @Test
    void getHistory_lanzaNotFoundSiLaAlertaNoExiste() {
        when(alertRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> alertService.getHistory(99L))
                .isInstanceOf(AlertNotFoundException.class);
    }

    @Test
    void getAlerts_rechazaSeveridadDesconocida() {
        AlertFilterRequest filter = new AlertFilterRequest(null, "EXTREME", null, null, null, null);

        assertThatThrownBy(() -> alertService.getAlerts(filter))
                .isInstanceOf(InvalidAlertDataException.class);
    }

    @Test
    void getAlerts_rechazaTipoDesconocido() {
        AlertFilterRequest filter = new AlertFilterRequest(null, null, "OTHER", null, null, null);

        assertThatThrownBy(() -> alertService.getAlerts(filter))
                .isInstanceOf(InvalidAlertDataException.class);
    }

    @Test
    void getAlerts_conListaVaciaDeSeccionesNoConsultaLaBaseDeDatos() {
        AlertFilterRequest filter = new AlertFilterRequest(null, null, null, null, List.of(), null);

        assertThat(alertService.getAlerts(filter)).isEmpty();

        verify(alertRepository, never()).findAll(
                org.mockito.ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<Alert>>any(),
                any(org.springframework.data.domain.Sort.class));
    }

    @Test
    void exists_yIsActive_sirvenParaC4() {
        when(alertRepository.existsById(10L)).thenReturn(true);
        when(alertRepository.existsById(99L)).thenReturn(false);
        when(alertRepository.findById(10L))
                .thenReturn(Optional.of(alertWithStatus(AlertStatus.IN_PROGRESS)));
        when(alertRepository.findById(11L))
                .thenReturn(Optional.of(alertWithStatus(AlertStatus.RESOLVED)));

        assertThat(alertService.exists(10L)).isTrue();
        assertThat(alertService.exists(99L)).isFalse();
        assertThat(alertService.exists(null)).isFalse();
        assertThat(alertService.isActive(10L)).isTrue();
        assertThat(alertService.isActive(11L)).isFalse();
    }

    @Test
    void getSummary_sumaTotalYActivas() {
        when(alertRepository.countByStatus(AlertStatus.NEW)).thenReturn(2L);
        when(alertRepository.countByStatus(AlertStatus.UNDER_REVIEW)).thenReturn(1L);
        when(alertRepository.countByStatus(AlertStatus.IN_PROGRESS)).thenReturn(1L);
        when(alertRepository.countByStatus(AlertStatus.RESOLVED)).thenReturn(3L);
        when(alertRepository.countByStatus(AlertStatus.DISMISSED)).thenReturn(1L);
        when(alertRepository.countByStatusInAndSeverity(AlertStatus.ACTIVE_STATUSES, "LOW")).thenReturn(1L);
        when(alertRepository.countByStatusInAndSeverity(AlertStatus.ACTIVE_STATUSES, "MEDIUM")).thenReturn(1L);
        when(alertRepository.countByStatusInAndSeverity(AlertStatus.ACTIVE_STATUSES, "HIGH")).thenReturn(2L);

        AlertSummaryResponse summary = alertService.getSummary();

        assertThat(summary.total()).isEqualTo(7L);
        assertThat(summary.active()).isEqualTo(4L);
        assertThat(summary.byStatus()).containsEntry("RESOLVED", 3L);
        assertThat(summary.activeBySeverity()).containsEntry("HIGH", 2L);
    }
}
