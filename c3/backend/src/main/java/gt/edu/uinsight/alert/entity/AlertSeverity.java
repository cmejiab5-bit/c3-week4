package gt.edu.uinsight.alert.entity;

import java.util.Arrays;
import java.util.Optional;

/**
 * Severidad de una alerta. Mismos valores que usa C2 (reglas), B7 (motor de
 * riesgo) y C5 (reportes, campo riskLevel).
 */
public enum AlertSeverity {

    LOW,
    MEDIUM,
    HIGH;

    public static Optional<AlertSeverity> parse(String value) {
        if (value == null) {
            return Optional.empty();
        }
        String normalized = value.trim();
        return Arrays.stream(values())
                .filter(s -> s.name().equalsIgnoreCase(normalized))
                .findFirst();
    }
}
