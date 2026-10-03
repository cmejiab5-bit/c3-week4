package gt.edu.uinsight.alert.service;

import gt.edu.uinsight.alert.dto.request.GenerateAlertRequest;
import gt.edu.uinsight.alert.dto.response.AlertGenerationResult;
import gt.edu.uinsight.alert.entity.Alert;
import gt.edu.uinsight.alert.entity.AlertStatus;
import gt.edu.uinsight.alert.entity.AlertStatusHistory;
import gt.edu.uinsight.alert.exception.InvalidAlertDataException;
import gt.edu.uinsight.alert.repository.AlertRepository;
import gt.edu.uinsight.alert.repository.AlertStatusHistoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertGenerationServiceTest {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private AlertStatusHistoryRepository historyRepository;

    @InjectMocks
    private AlertGenerationService generationService;

    private GenerateAlertRequest validRequest() {
        return new GenerateAlertRequest(
                10L, "trend", "high",
                "  Tendencia negativa  ", "El promedio bajó 10 puntos"
        );
    }

    @Test
    void generate_creaAlertaNuevaEnEstadoNew() {
        when(alertRepository.findFirstBySectionIdAndAlertTypeAndStatusIn(
                eq(10L), eq("TREND"), anyCollection())).thenReturn(Optional.empty());
        when(alertRepository.save(any(Alert.class))).thenAnswer(i -> i.getArgument(0));

        AlertGenerationResult result = generationService.generate(validRequest());

        assertThat(result.created()).isTrue();
        assertThat(result.alert().status()).isEqualTo(AlertStatus.NEW);
        assertThat(result.alert().alertType()).isEqualTo("TREND");
        assertThat(result.alert().severity()).isEqualTo("HIGH");
        assertThat(result.alert().title()).isEqualTo("Tendencia negativa");
        verify(historyRepository).save(any(AlertStatusHistory.class));
    }

    @Test
    void generate_noDuplicaSiYaHayAlertaAbiertaDelMismoTipo() {
        Alert existing = new Alert(10L, "TREND", "HIGH", "Existente", null);
        when(alertRepository.findFirstBySectionIdAndAlertTypeAndStatusIn(
                eq(10L), eq("TREND"), anyCollection())).thenReturn(Optional.of(existing));

        AlertGenerationResult result = generationService.generate(validRequest());

        assertThat(result.created()).isFalse();
        assertThat(result.alert().title()).isEqualTo("Existente");
        verify(alertRepository, never()).save(any(Alert.class));
        verify(historyRepository, never()).save(any(AlertStatusHistory.class));
    }

    @Test
    void generate_rechazaTipoInvalido() {
        GenerateAlertRequest request =
                new GenerateAlertRequest(10L, "OTHER", "HIGH", "Titulo", null);

        assertThatThrownBy(() -> generationService.generate(request))
                .isInstanceOf(InvalidAlertDataException.class);
    }

    @Test
    void generate_rechazaSeveridadInvalida() {
        GenerateAlertRequest request =
                new GenerateAlertRequest(10L, "TREND", "EXTREME", "Titulo", null);

        assertThatThrownBy(() -> generationService.generate(request))
                .isInstanceOf(InvalidAlertDataException.class);
    }

    @Test
    void generate_rechazaSeccionNulaYTituloVacio() {
        assertThatThrownBy(() -> generationService.generate(
                new GenerateAlertRequest(null, "TREND", "HIGH", "Titulo", null)))
                .isInstanceOf(InvalidAlertDataException.class);

        assertThatThrownBy(() -> generationService.generate(
                new GenerateAlertRequest(10L, "TREND", "HIGH", "   ", null)))
                .isInstanceOf(InvalidAlertDataException.class);
    }

    @Test
    void generate_rechazaTituloMasLargoQueLaColumna() {
        String longTitle = "x".repeat(151);

        assertThatThrownBy(() -> generationService.generate(
                new GenerateAlertRequest(10L, "TREND", "HIGH", longTitle, null)))
                .isInstanceOf(InvalidAlertDataException.class);
    }
}
