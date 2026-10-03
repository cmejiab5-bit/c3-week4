package gt.edu.uinsight.alert.exception;

/**
 * Datos inválidos al pedir una alerta o un filtro (tipo o severidad
 * desconocidos, título vacío, sección nula, etc.). Se responde con HTTP 400.
 */
public class InvalidAlertDataException extends RuntimeException {

    public InvalidAlertDataException(String message) {
        super(message);
    }
}
