-- Célula C3 - Ciclo de vida de las alertas
-- SOLO PARA DESARROLLO LOCAL: alertas de prueba para ejercer la API de C3
-- (y la de C4) sin esperar al motor de riesgos B7.
--
-- Requiere las tablas alert y alert_status_history (08_create_alert.sql).
-- No requiere que exista la tabla section: section_id es un número suelto
-- mientras no se aplique 11_alter_alert_add_section_fk.sql.
-- No ejecutar junto con local-dev/stub_alert_for_c4_testing.sql (ese stub crea
-- su propia tabla alert con otras columnas).

INSERT INTO alert (id, section_id, alert_type, severity, title, description, status, generated_at, resolved_at)
VALUES
 (101, 1, 'PERFORMANCE', 'HIGH',   'Alto porcentaje de estudiantes bajo el umbral', '52% de la sección está por debajo de 61 puntos.', 'NEW',          CURRENT_TIMESTAMP, NULL),
 (102, 1, 'TREND',       'HIGH',   'Tendencia negativa en las últimas evaluaciones', 'El promedio bajó de 78 a 68 en tres evaluaciones.', 'UNDER_REVIEW',  CURRENT_TIMESTAMP, NULL),
 (103, 2, 'DISPERSION',  'MEDIUM', 'Alta dispersión en los resultados',              'Desviación estándar de 18.4 en la última evaluación.', 'IN_PROGRESS',  CURRENT_TIMESTAMP, NULL),
 (104, 2, 'TREND',       'LOW',    'Ligera baja en el promedio',                      NULL,                                                   'RESOLVED',     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
 (105, 3, 'PERFORMANCE', 'MEDIUM', 'Rendimiento bajo en la sección',                  NULL,                                                   'DISMISSED',    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO alert_status_history (alert_id, from_status, to_status, comment, changed_at)
VALUES
 (101, NULL,           'NEW',          'Alerta generada', CURRENT_TIMESTAMP),
 (102, NULL,           'NEW',          'Alerta generada', CURRENT_TIMESTAMP),
 (102, 'NEW',          'UNDER_REVIEW', 'Se revisa con el docente', CURRENT_TIMESTAMP),
 (103, NULL,           'NEW',          'Alerta generada', CURRENT_TIMESTAMP),
 (103, 'NEW',          'UNDER_REVIEW', NULL, CURRENT_TIMESTAMP),
 (103, 'UNDER_REVIEW', 'IN_PROGRESS',  'Plan de tutorías definido', CURRENT_TIMESTAMP),
 (104, NULL,           'NEW',          'Alerta generada', CURRENT_TIMESTAMP),
 (104, 'NEW',          'UNDER_REVIEW', NULL, CURRENT_TIMESTAMP),
 (104, 'UNDER_REVIEW', 'IN_PROGRESS',  NULL, CURRENT_TIMESTAMP),
 (104, 'IN_PROGRESS',  'RESOLVED',     'Promedio recuperado', CURRENT_TIMESTAMP),
 (105, NULL,           'NEW',          'Alerta generada', CURRENT_TIMESTAMP),
 (105, 'NEW',          'DISMISSED',    'Falso positivo: faltaba cargar una evaluación', CURRENT_TIMESTAMP);