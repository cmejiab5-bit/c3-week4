package gt.edu.uinsight.alert.dto.response;

/**
 * Resultado de pedirle a C3 que genere una alerta (lo usa B7).
 *
 * created = true  -> se creó una alerta nueva.
 * created = false -> ya existía una alerta abierta del mismo tipo para la
 *                    sección; alert es la existente (no se duplicó).
 */
public record AlertGenerationResult(

        boolean created,
        AlertResponse alert

) {
}
