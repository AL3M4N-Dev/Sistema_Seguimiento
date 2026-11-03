package modelo;

/**
 * Estados de una solicitud de constancia (columna solicitudes_constancia.estado).
 * Flujo: solicitada → validada (el responsable revisó las horas) → disponible (ya hay PDF).
 * Desde solicitada o validada se puede pasar a rechazada.
 */
public enum EstadoSolicitud {
    SOLICITADA("solicitada", "Solicitada"),
    VALIDADA("validada", "Validada"),
    DISPONIBLE("disponible", "Disponible"),
    RECHAZADA("rechazada", "Rechazada");

    private final String valorBd;
    private final String etiqueta;

    EstadoSolicitud(String valorBd, String etiqueta) {
        this.valorBd = valorBd;
        this.etiqueta = etiqueta;
    }

    public String getValorBd() {
        return valorBd;
    }

    public static EstadoSolicitud desdeBd(String valor) {
        for (EstadoSolicitud e : values()) {
            if (e.valorBd.equals(valor)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Estado de solicitud desconocido: " + valor);
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
