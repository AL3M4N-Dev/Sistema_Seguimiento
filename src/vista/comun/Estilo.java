package vista.comun;

import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;

/** Colores y componentes con el mismo estilo en todas las pantallas. */
public final class Estilo {

    public static final Color AZUL = new Color(0x1D4ED8);
    public static final Color TEXTO = new Color(0x1A1D21);
    public static final Color TEXTO_SECUNDARIO = new Color(0x5B6370);
    public static final Color FONDO = new Color(0xF4F5F7);
    public static final Color BORDE = new Color(0xE3E6EA);

    public static final Color PENDIENTE_FONDO = new Color(0xFEF3C7);
    public static final Color PENDIENTE_TEXTO = new Color(0x92400E);
    public static final Color APROBADO_FONDO = new Color(0xDCFCE7);
    public static final Color APROBADO_TEXTO = new Color(0x166534);
    public static final Color RECHAZADO_FONDO = new Color(0xFEE2E2);
    public static final Color RECHAZADO_TEXTO = new Color(0x991B1B);

    public static final Color VALIDADA_FONDO = new Color(0xDBEAFE);
    public static final Color VALIDADA_TEXTO = new Color(0x1E40AF);

    public static final Color VERDE = new Color(0x15803D);
    public static final Color ROJO = new Color(0xB91C1C);

    private Estilo() {
    }

    /**
     * Colores {fondo, texto} para una etiqueta de estado, o null si no es un estado.
     * Amarillo = pendiente/solicitada, azul = validada, verde = aprobado/disponible,
     * rojo = rechazado.
     */
    public static Color[] coloresEstado(String etiqueta) {
        String e = etiqueta == null ? "" : etiqueta.toLowerCase();
        if (e.equals("pendiente") || e.equals("solicitada")) {
            return new Color[]{PENDIENTE_FONDO, PENDIENTE_TEXTO};
        }
        if (e.equals("validada") || e.equals("lista para subir")) {
            return new Color[]{VALIDADA_FONDO, VALIDADA_TEXTO};
        }
        if (e.equals("aprobado") || e.equals("disponible") || e.equals("entregada") || e.equals("sí")) {
            return new Color[]{APROBADO_FONDO, APROBADO_TEXTO};
        }
        if (e.equals("rechazado") || e.equals("rechazada") || e.startsWith("no,")) {
            return new Color[]{RECHAZADO_FONDO, RECHAZADO_TEXTO};
        }
        return null;
    }

    public static JLabel titulo(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 20f));
        l.setForeground(TEXTO);
        return l;
    }

    public static JLabel subtitulo(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 15f));
        l.setForeground(TEXTO);
        return l;
    }

    public static JLabel secundario(String texto) {
        JLabel l = new JLabel(texto);
        l.setForeground(TEXTO_SECUNDARIO);
        return l;
    }

    /** Botón relleno de color (Aprobar, Emitir). */
    public static JButton botonPrincipal(String texto, Color color) {
        JButton b = new JButton(texto);
        b.setBackground(color);
        b.setForeground(Color.WHITE);
        b.setFont(b.getFont().deriveFont(Font.BOLD));
        return b;
    }

    /** Botón con borde de color (Rechazar). */
    public static JButton botonContorno(String texto, Color color) {
        JButton b = new JButton(texto);
        b.setForeground(color);
        b.setFont(b.getFont().deriveFont(Font.BOLD));
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color, 1, true),
                BorderFactory.createEmptyBorder(6, 14, 6, 14)));
        return b;
    }
}
