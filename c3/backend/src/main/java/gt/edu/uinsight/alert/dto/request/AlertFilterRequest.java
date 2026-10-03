package gt.edu.uinsight.alert.dto.request;

import gt.edu.uinsight.alert.entity.AlertStatus;

import java.util.Collection;

/**
 * Criterios de búsqueda de alertas. Todos son opcionales.
 *
 * sectionIds no se expone por HTTP: lo usan otros módulos (por ejemplo C5,
 * para filtrar por curso, docente o período) llamando al servicio.
 */
public record AlertFilterRequest(
        AlertStatus status,
        String severity,
        String alertType,
        Long sectionId,
        Collection<Long> sectionIds,
        Boolean activeOnly
) {

    public static AlertFilterRequest none() {
        return new AlertFilterRequest(null, null, null, null, null, null);
    }
}
