package vista.docente;

import java.awt.Component;
import java.awt.Font;
import java.sql.SQLException;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import modelo.AlumnoActivo;
import modelo.RegistroHoras;
import servicio.PanelDocenteService;
import vista.comun.Avatar;
import vista.comun.Boton;
import vista.comun.Dialogos;
import vista.comun.EstadoRenderer;
import vista.comun.Estilo;
import vista.comun.Formato;
import vista.comun.ListaTableModel;
import vista.comun.VentanaDialogo;

/** Ventana con TODOS los registros de horas de un alumno (aprobados, pendientes y rechazados). */
public class RegistrosAlumnoDialog extends VentanaDialogo {

    public static void mostrar(Component padre, PanelDocenteService servicio, AlumnoActivo alumno) {
        try {
            List<RegistroHoras> registros = servicio.registrosDe(alumno.getIdInscripcion());
            new RegistrosAlumnoDialog(padre, alumno, registros).mostrar();
        } catch (SQLException e) {
            Dialogos.mostrarError(padre, e);
        }
    }

    private RegistrosAlumnoDialog(Component padre, AlumnoActivo alumno, List<RegistroHoras> registros) {
        super(padre, "Registros de " + alumno.getNombre(),
                registros.size() + (registros.size() == 1 ? " registro" : " registros") + "  ·  " + alumno.getInstitucion());
        setIcono(new Avatar(alumno.getNombre(), 44));
        setSeparador(true);
        setMargenContenido(0, 0, 0, 0);

        if (registros.isEmpty()) {
            JLabel vacio = Estilo.secundario("Este alumno todavía no tiene registros de horas.");
            vacio.setHorizontalAlignment(SwingConstants.CENTER);
            setContenido(vacio);
        } else {
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
            JTable tabla = new JTable(modelo) {
                @Override
                public String getToolTipText(java.awt.event.MouseEvent e) {
                    int fila = rowAtPoint(e.getPoint());
                    int columna = columnAtPoint(e.getPoint());
                    Object valor = fila < 0 || columna < 0 ? null : getValueAt(fila, columna);
                    return columna == 4 && valor != null && !valor.toString().isEmpty() ? valor.toString() : null;
                }
            };
            Estilo.estilizarTabla(tabla);
            tabla.getColumnModel().getColumn(3).setCellRenderer(new EstadoRenderer());
            int[] anchos = {100, 120, 70, 120, 320};
            for (int i = 0; i < anchos.length; i++) {
                tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
            }
            tabla.getAccessibleContext().setAccessibleName("Registros de " + alumno.getNombre());
            setContenido(Estilo.scroll(tabla));
        }

        setPie(Estilo.etiqueta("Aprobadas: " + Formato.horas(alumno.getHorasAprobadas()) + " de "
                + alumno.getHorasRequeridas() + " h  ·  En revisión: " + Formato.horas(alumno.getHorasPendientes()) + " h",
                Estilo.fuente(Font.PLAIN, 13), Estilo.TEXTO_SECUNDARIO));
        Boton cerrar = agregarBotonCancelar("Cerrar");
        setBotonPorDefecto(cerrar);
        setTamano(860, 560);
    }
}
