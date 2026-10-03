package gt.edu.uinsight.alert.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AlertStatusTest {

    @Test
    void newPuedePasarARevisionODescartada() {
        assertThat(AlertStatus.NEW.allowedTransitions())
                .containsExactly(AlertStatus.UNDER_REVIEW, AlertStatus.DISMISSED);
    }

    @Test
    void newNoPuedePasarDirectoAResuelta() {
        assertThat(AlertStatus.NEW.canTransitionTo(AlertStatus.RESOLVED)).isFalse();
        assertThat(AlertStatus.NEW.canTransitionTo(AlertStatus.IN_PROGRESS)).isFalse();
    }

    @Test
    void enProgresoSoloPuedeResolverse() {
        assertThat(AlertStatus.IN_PROGRESS.allowedTransitions())
                .containsExactly(AlertStatus.RESOLVED);
    }

    @Test
    void estadosFinalesNoTienenTransiciones() {
        assertThat(AlertStatus.RESOLVED.allowedTransitions()).isEmpty();
        assertThat(AlertStatus.DISMISSED.allowedTransitions()).isEmpty();
        assertThat(AlertStatus.RESOLVED.isFinal()).isTrue();
        assertThat(AlertStatus.DISMISSED.isFinal()).isTrue();
    }

    @Test
    void soloNewRevisionYEnProgresoSonActivos() {
        assertThat(AlertStatus.ACTIVE_STATUSES)
                .containsExactly(AlertStatus.NEW, AlertStatus.UNDER_REVIEW, AlertStatus.IN_PROGRESS);
        assertThat(AlertStatus.RESOLVED.isActive()).isFalse();
        assertThat(AlertStatus.DISMISSED.isActive()).isFalse();
    }

    @Test
    void severidadYTipoSeInterpretanSinImportarMayusculas() {
        assertThat(AlertSeverity.parse(" high ")).contains(AlertSeverity.HIGH);
        assertThat(AlertSeverity.parse("critica")).isEmpty();
        assertThat(AlertType.parse("trend")).contains(AlertType.TREND);
        assertThat(AlertType.parse(null)).isEmpty();
    }
}
