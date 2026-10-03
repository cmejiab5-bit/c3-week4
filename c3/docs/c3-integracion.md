# Célula C3 — Ciclo de vida de las alertas: contrato e integración

Código: `backend/src/main/java/gt/edu/uinsight/alert/`  
Tablas: `alert`, `alert_status_history` (`database/scripts/08_create_alert.sql`)

## 1. Estados y transiciones

```
NEW ──► UNDER_REVIEW ──► IN_PROGRESS ──► RESOLVED
 │            │
 └────────────┴──► DISMISSED
```

`RESOLVED` y `DISMISSED` son finales. Cualquier otra transición (por ejemplo `NEW → RESOLVED`) responde **409**.
Activas = `NEW`, `UNDER_REVIEW`, `IN_PROGRESS` (`AlertStatus.ACTIVE_STATUSES`).

Valores permitidos (los mismos que usan C2, B7 y C5):

| Campo | Valores |
|---|---|
| `severity` | `LOW`, `MEDIUM`, `HIGH` |
| `alertType` | `PERFORMANCE`, `TREND`, `DISPERSION` |

## 2. API REST (`/api/v1/alerts`)

| Método | Ruta | Descripción | Códigos |
|---|---|---|---|
| GET | `/alerts` | Lista, más recientes primero. Filtros opcionales: `status`, `severity`, `alertType`, `sectionId` | 200, 400 |
| GET | `/alerts/active` | Alertas abiertas | 200 |
| GET | `/alerts/summary` | Conteo por estado y activas por severidad | 200 |
| GET | `/alerts/{id}` | Detalle (incluye `active` y `allowedTransitions`) | 200, 404 |
| GET | `/alerts/{id}/history` | Historial de cambios de estado | 200, 404 |
| PATCH | `/alerts/{id}/status` | Body `{ "status": "...", "comment": "..." }` (`comment` opcional) | 200, 400, 404, 409 |

Errores con el formato común del proyecto: `{ timestamp, status, error, message, details, traceId }`.
Códigos `error`: `ALERT_NOT_FOUND`, `INVALID_STATUS_TRANSITION`, `INVALID_ALERT_DATA`, `VALIDATION_ERROR`, `UNEXPECTED_ERROR`.

Respuesta de una alerta:

```json
{
  "id": 10, "sectionId": 1, "alertType": "TREND", "severity": "HIGH",
  "title": "Tendencia negativa", "description": "...",
  "status": "NEW", "active": true,
  "allowedTransitions": ["UNDER_REVIEW", "DISMISSED"],
  "generatedAt": "2026-09-20T10:00:00", "resolvedAt": null
}
```

## 3. Cómo se integra con cada célula

### B7 — Motor de riesgos (escribe alertas)
No hay endpoint POST: B7 está en el mismo monolito y llama al servicio. Esto valida los datos y **evita duplicados**
(si la sección ya tiene una alerta abierta del mismo tipo devuelve la existente con `created=false`).

```java
// En RiskEngineService (inyectar AlertGenerationService)
AlertGenerationResult r = alertGenerationService.generate(new GenerateAlertRequest(
        sectionId, "TREND", "HIGH",
        "Tendencia negativa en la sección",
        "El promedio bajó de 78 a 68 en tres evaluaciones"));

if (r.created()) alertsGenerated++;   // r.alert() trae la alerta (nueva o existente)
```

`alertType`/`severity` aceptan mayúsculas o minúsculas. Un tipo o severidad desconocidos lanzan `InvalidAlertDataException`.

### C4 — Intervenciones
C4 hoy valida la alerta con `AlertValidationJdbcAdapter` (SQL directo a la tabla `alert`). **Ya es compatible** con la tabla de C3:
usa `id` y `status`, y trata `RESOLVED`/`DISMISSED` como no activas, igual que C3. No necesita cambios para funcionar.
Cuando C4 quiera dejar el SQL (BKL-07), puede reemplazar solo esa clase por esta, sin tocar `InterventionService`:

```java
@Component
public class AlertValidationJdbcAdapter implements AlertValidationPort {
    private final gt.edu.uinsight.alert.service.AlertService alertService;
    public AlertValidationJdbcAdapter(gt.edu.uinsight.alert.service.AlertService alertService) {
        this.alertService = alertService;
    }
    @Override public boolean exists(Long alertId)   { return alertService.exists(alertId); }
    @Override public boolean isActive(Long alertId) { return alertService.isActive(alertId); }
}
```

### C5 — Reportes
Hoy usa `MockAlert`. Para pasar a datos reales puede inyectar `AlertService`:

- `getAlerts(AlertFilterRequest)` acepta `sectionIds` (lista) para filtrar por período, curso o docente: C5 obtiene
  los ids de sección que cumplen el filtro y se los pasa a C3.
- `getSummary()` da `active` (alertas activas) y `activeBySeverity` (`HIGH` → secciones/alertas de riesgo alto).
- Mapeo: C5 `type` = `alertType`, C5 `riskLevel` = `severity`.

### C6 — Frontend
Usar `GET /alerts` (con filtros), `GET /alerts/{id}`, `GET /alerts/{id}/history`, `PATCH /alerts/{id}/status` y
`GET /alerts/summary` para el inicio. `allowedTransitions` indica qué botones mostrar en cada alerta.

### C7 — Calidad
`IntegrationStatusService` solo prueba `/api/v1/alerts/1/interventions`, que da 404 si no existe la alerta 1.
Sugerencia a C7: agregar `/api/v1/alerts` (o `/api/v1/alerts/summary`) como chequeo del módulo de alertas.

## 4. Base de datos
- `08_create_alert.sql`: tablas `alert` y `alert_status_history`.
- `11_alter_alert_add_section_fk.sql`: llave foránea `alert.section_id → section(id)` del contrato oficial; ejecutar cuando A4 publique `section`.
- `local-dev/seed_c3_alerts.sql`: 5 alertas de prueba (ids 101–105) para usar sin B7. No combinar con `stub_alert_for_c4_testing.sql`.

## 5. Pruebas
`AlertStatusTest`, `AlertServiceTest`, `AlertGenerationServiceTest`, `AlertControllerTest` (JUnit 5 + Mockito + MockMvc standalone).
Ejecutar: `./mvnw -f backend/pom.xml test -Dtest='Alert*Test'`.

## 6. Pendientes / decisiones
- El manejador de errores de C3 está acotado a `AlertController` y con prioridad máxima; si el coordinador unifica los
  manejadores globales, hay que registrar ahí las excepciones de `alert.exception`.
- Anti-duplicados: la verificación y el insert no son atómicos; si B7 se ejecuta en paralelo sobre la misma sección
  podría crearse una alerta repetida. Para el MVP (evaluación manual) es aceptable.
- No se agregó paginación en `GET /alerts`; se puede añadir si el volumen lo requiere.
