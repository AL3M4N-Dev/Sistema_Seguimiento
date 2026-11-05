package modelo;

/** Estados de un registro de horas (columna registros_horas.estado). */
public enum EstadoRegistro {
    PENDIENTE("pendiente", "Pendiente"),
    APROBADO("aprobado", "Aprobado"),
    RECHAZADO("rechazado", "Rechazado");

    private final String valorBd;
    private final String etiqueta;

    EstadoRegistro(String valorBd, String etiqueta) {
        this.valorBd = valorBd;
        this.etiqueta = etiqueta;
    }

    public String getValorBd() {
        return valorBd;
    }

    public static EstadoRegistro desdeBd(String valor) {
        for (EstadoRegistro e : values()) {
            if (e.valorBd.equals(valor)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Estado de registro desconocido: " + valor);
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
