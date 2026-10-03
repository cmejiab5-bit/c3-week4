package gt.edu.uinsight.alert.exception;

import gt.edu.uinsight.alert.entity.AlertStatus;

public class InvalidAlertStatusTransitionException
        extends RuntimeException {

    public InvalidAlertStatusTransitionException(
            AlertStatus current,
            AlertStatus requested
    ) {
        super(
                "No se permite cambiar el estado de "
                        + current
                        + " a "
                        + requested
        );
    }
}

