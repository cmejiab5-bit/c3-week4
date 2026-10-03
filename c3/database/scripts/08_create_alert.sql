-- Célula C3 - Ciclo de vida de las alertas
-- Tablas responsables: alert, alert_status_history
--
-- Nota: la llave foránea alert.section_id -> section(id) del contrato oficial
-- (sección 8.3 del documento) se agrega en 11_alter_alert_add_section_fk.sql,
-- que debe ejecutarse cuando ya exista la tabla section (célula A4).

CREATE TABLE IF NOT EXISTS alert (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    section_id BIGINT NOT NULL,

    alert_type VARCHAR(50) NOT NULL,

    severity VARCHAR(20) NOT NULL,

    title VARCHAR(150) NOT NULL,

    description VARCHAR(500),

    status VARCHAR(30) NOT NULL,

    generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    resolved_at TIMESTAMP NULL,

    CONSTRAINT chk_alert_status
        CHECK (status IN (
                'NEW',
                'UNDER_REVIEW',
                'IN_PROGRESS',
                'RESOLVED',
                'DISMISSED'
            )
        )
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_alert_section_id
    ON alert(section_id);

CREATE INDEX idx_alert_status
    ON alert(status);

CREATE INDEX idx_alert_generated_at
    ON alert(generated_at);

-- Bitácora de cambios de estado (la primera fila de cada alerta tiene
-- from_status NULL: es la creación).
CREATE TABLE IF NOT EXISTS alert_status_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    alert_id BIGINT NOT NULL,

    from_status VARCHAR(30) NULL,

    to_status VARCHAR(30) NOT NULL,

    comment VARCHAR(255),

    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_alert_status_history_alert
        FOREIGN KEY (alert_id) REFERENCES alert(id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_alert_status_history_alert_id
    ON alert_status_history(alert_id);
