package gt.edu.uinsight.alert.entity;

import java.util.List;

/**
 * Estados del ciclo de vida de una alerta (célula C3).
 *
 * NEW -> UNDER_REVIEW -> IN_PROGRESS -> RESOLVED
 * (o DISMISSED desde NEW o UNDER_REVIEW)
 *
 * RESOLVED y DISMISSED son estados finales. No se permiten saltos
 * (por ejemplo NEW -> RESOLVED).
 */
public enum AlertStatus {

    NEW,
    UNDER_REVIEW,
    IN_PROGRESS,
    RESOLVED,
    DISMISSED;

    /** Estados en los que la alerta sigue abierta (la usan C4, C5 y C6). */
    public static final List<AlertStatus> ACTIVE_STATUSES =
            List.of(NEW, UNDER_REVIEW, IN_PROGRESS);

    public boolean isActive() {
        return ACTIVE_STATUSES.contains(this);
    }

    public boolean isFinal() {
        return !isActive();
    }

    /** Estados a los que se puede pasar desde el estado actual. */
    public List<AlertStatus> allowedTransitions() {
        return switch (this) {
            case NEW -> List.of(UNDER_REVIEW, DISMISSED);
            case UNDER_REVIEW -> List.of(IN_PROGRESS, DISMISSED);
            case IN_PROGRESS -> List.of(RESOLVED);
            case RESOLVED, DISMISSED -> List.of();
        };
    }

    public boolean canTransitionTo(AlertStatus requested) {
        return allowedTransitions().contains(requested);
    }
}
