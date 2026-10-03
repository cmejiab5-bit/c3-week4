package gt.edu.uinsight.alert.exception;

public class AlertNotFoundException extends RuntimeException {

    public AlertNotFoundException(Long id) {
        super("No se encontró la alerta con id: " + id);
    }
}