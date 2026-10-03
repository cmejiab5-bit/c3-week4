package gt.edu.uinsight.alert.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Bitácora de cambios de estado de una alerta (tabla alert_status_history).
 * El primer registro de cada alerta tiene fromStatus = null (creación).
 */
@Entity
@Table(name = "alert_status_history")
public class AlertStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "alert_id", nullable = false)
    private Long alertId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 30)
    private AlertStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 30)
    private AlertStatus toStatus;

    @Column(length = 255)
    private String comment;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    protected AlertStatusHistory() {
        // Requerido por JPA
    }

    public AlertStatusHistory(
            Long alertId,
            AlertStatus fromStatus,
            AlertStatus toStatus,
            String comment
    ) {
        this.alertId = alertId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.comment = comment;
        this.changedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getAlertId() {
        return alertId;
    }

    public AlertStatus getFromStatus() {
        return fromStatus;
    }

    public AlertStatus getToStatus() {
        return toStatus;
    }

    public String getComment() {
        return comment;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }
}
