package gt.edu.uinsight.alert.entity;

import java.util.Arrays;
import java.util.Optional;

/**
 * Tipos de alerta contemplados en el documento del proyecto (sección 4.4)
 * y usados por C5 en sus reportes.
 *
 * PERFORMANCE: alto porcentaje de estudiantes con rendimiento bajo.
 * TREND:       el promedio disminuyó en las últimas evaluaciones.
 * DISPERSION:  variabilidad significativa entre los resultados.
 */
public enum AlertType {

    PERFORMANCE,
    TREND,
    DISPERSION;

    public static Optional<AlertType> parse(String value) {
        if (value == null) {
            return Optional.empty();
        }
        String normalized = value.trim();
        return Arrays.stream(values())
                .filter(t -> t.name().equalsIgnoreCase(normalized))
                .findFirst();
    }
}
