package vista.docente;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.sql.SQLException;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import modelo.AlumnoActivo;
import modelo.RegistroHoras;
import servicio.PanelDocenteService;
import vista.comun.Boton;
import vista.comun.Dialogos;
import vista.comun.EstadoRenderer;
import vista.comun.Estilo;
import vista.comun.Formato;
import vista.comun.ListaTableModel;
import vista.comun.Tarjeta;

/** Ventana con TODOS los registros de horas de un alumno (aprobados, pendientes y rechazados). */
public class RegistrosAlumnoDialog extends JDialog {

    public static void mostrar(Component padre, PanelDocenteService servicio, AlumnoActivo alumno) {
        try {
            List<RegistroHoras> registros = servicio.registrosDe(alumno.getIdInscripcion());
            new RegistrosAlumnoDialog(padre, alumno, registros).setVisible(true);
        } catch (SQLException e) {
            Dialogos.mostrarError(padre, e);
        }
    }

    private RegistrosAlumnoDialog(Component padre, AlumnoActivo alumno, List<RegistroHoras> registros) {
        super(SwingUtilities.getWindowAncestor(padre), "Registros de " + alumno.getNombre(), ModalityType.APPLICATION_MODAL);

        ListaTableModel<RegistroHoras> modelo = new ListaTableModel<>(
                new String[]{"Fecha", "Horario", "Horas", "Estado", "Comentario del docente"},
                (r, col) -> switch (col) {
                    case 0 -> Formato.fecha(r.getFecha());
                    case 1 -> Formato.horario(r.getHoraInicio(), r.getHoraFin());
                    case 2 -> Formato.horas(r.getHorasCalculadas()) + " h";
                    case 3 -> r.getEstado();
                    default -> r.getComentarioDocente() == null ? "" : r.getComentarioDocente();
                });
        modelo.setFilas(registros);
        JTable tabla = new JTable(modelo);
        Estilo.estilizarTabla(tabla);
        tabla.getColumnModel().getColumn(3).setCellRenderer(new EstadoRenderer());
        int[] anchos = {100, 120, 70, 120, 320};
        for (int i = 0; i < anchos.length; i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }

        Tarjeta marco = new Tarjeta(new BorderLayout(0, 10));
        marco.add(Estilo.subtitulo(registros.size() + " registro(s) · " + alumno.getInstitucion()), BorderLayout.NORTH);
        marco.add(Estilo.scroll(tabla), BorderLayout.CENTER);

        Boton cerrar = Estilo.botonSecundario("Cerrar");
        cerrar.addActionListener(e -> dispose());
        JPanel pie = Estilo.panel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        pie.add(cerrar);

        JPanel contenido = new JPanel(new BorderLayout(0, 14));
        contenido.setBackground(Estilo.FONDO);
        contenido.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        contenido.add(marco, BorderLayout.CENTER);
        contenido.add(pie, BorderLayout.SOUTH);
        setContentPane(contenido);
        setSize(820, 540);
        setLocationRelativeTo(padre);
    }
}
