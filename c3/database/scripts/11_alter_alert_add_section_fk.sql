-- Célula C3 - Ciclo de vida de las alertas
-- Agrega la llave foránea alert.section_id -> section(id) definida en el
-- contrato oficial del modelo de datos (sección 8.3).
--
-- Ejecutar DESPUÉS de crear la tabla section (célula A4) y 08_create_alert.sql.
-- Si ya existen alertas con section_id que no existe en section, este script
-- falla: corregir esos datos primero.

ALTER TABLE alert
    ADD CONSTRAINT fk_alert_section
        FOREIGN KEY (section_id) REFERENCES section(id);
