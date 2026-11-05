package modelo;

/** Convierte el tipo de programa de la base ('servicio_social' | 'practicas') a texto. */
public final class Programa {

    private Programa() {
    }

    public static String texto(String tipo) {
        if ("practicas".equals(tipo)) {
            return "Prácticas";
        }
        if ("servicio_social".equals(tipo)) {
            return "Servicio social";
        }
        return tipo == null ? "" : tipo;
    }
}
