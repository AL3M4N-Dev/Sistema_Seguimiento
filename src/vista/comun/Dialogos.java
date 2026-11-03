package vista.comun;

import servicio.ReglaNegocioException;
import java.awt.BorderLayout;
import java.awt.Component;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/** Ventanas de mensaje que usan todas las pantallas. */
public final class Dialogos {

    private Dialogos() {
    }

    /** Pide un comentario. Devuelve null si el usuario cancela. */
    public static String pedirComentario(Component padre, String titulo, String mensaje) {
        JTextArea area = new JTextArea(5, 36);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.add(new JLabel(mensaje), BorderLayout.NORTH);
        panel.add(new JScrollPane(area), BorderLayout.CENTER);
        int r = JOptionPane.showConfirmDialog(padre, panel, titulo,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        return r == JOptionPane.OK_OPTION ? area.getText() : null;
    }

    public static boolean confirmar(Component padre, String mensaje) {
        return JOptionPane.showConfirmDialog(padre, mensaje, "Confirmar",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    public static void info(Component padre, String mensaje) {
        JOptionPane.showMessageDialog(padre, mensaje, "Listo", JOptionPane.INFORMATION_MESSAGE);
    }

    /** Una regla no cumplida se muestra como aviso; cualquier otro error, como error. */
    public static void mostrarError(Component padre, Exception e) {
        if (e instanceof ReglaNegocioException) {
            JOptionPane.showMessageDialog(padre, e.getMessage(), "Revisa esto", JOptionPane.WARNING_MESSAGE);
        } else {
            e.printStackTrace();
            JOptionPane.showMessageDialog(padre,
                    "No se pudo completar la acción.\n\nDetalle: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
