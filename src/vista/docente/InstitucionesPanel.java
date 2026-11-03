package vista.docente;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.sql.SQLException;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import modelo.Institucion;
import servicio.PanelDocenteService;
import servicio.ReglaNegocioException;
import vista.comun.Dialogos;
import vista.comun.Estilo;
import vista.comun.Formato;
import vista.comun.ListaTableModel;

/** Pestaña 2: instituciones de prácticas que dieron de alta los alumnos. */
public class InstitucionesPanel extends JPanel {

    private final PanelDocenteService servicio;
    private final ListaTableModel<Institucion> modelo = new ListaTableModel<>(
            new String[]{"Institución", "Registrada por", "Dirección", "Contacto", "Horas", "Fecha de alta"},
            (i, col) -> switch (col) {
                case 0 -> i.getNombre();
                case 1 -> i.getCreadaPorNombre();
                case 2 -> i.getDireccion();
                case 3 -> i.getContacto();
                case 4 -> i.getHorasRequeridas() + " h";
                default -> Formato.fechaHora(i.getFechaAlta());
            });
    private final JTable tabla = new JTable(modelo);
    private final JLabel resumen = Estilo.secundario(" ");

    public InstitucionesPanel(PanelDocenteService servicio) {
        this.servicio = servicio;
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JButton actualizar = new JButton("Actualizar");
        JButton rechazar = Estilo.botonContorno("Rechazar", Estilo.ROJO);
        JButton aprobar = Estilo.botonPrincipal("Aprobar", Estilo.VERDE);

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        acciones.add(actualizar);
        acciones.add(rechazar);
        acciones.add(aprobar);

        JPanel textos = new JPanel(new BorderLayout(0, 4));
        textos.add(Estilo.subtitulo("Instituciones de prácticas por aprobar"), BorderLayout.NORTH);
        textos.add(Estilo.secundario("Mientras una institución esté en revisión, el alumno no puede registrar horas."),
                BorderLayout.SOUTH);

        JPanel arriba = new JPanel(new BorderLayout());
        arriba.add(textos, BorderLayout.WEST);
        arriba.add(acciones, BorderLayout.EAST);

        tabla.setRowHeight(32);
        tabla.setAutoCreateRowSorter(true);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        int[] anchos = {200, 140, 200, 200, 60, 130};
        for (int i = 0; i < anchos.length; i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }

        add(arriba, BorderLayout.NORTH);
        add(new JScrollPane(tabla), BorderLayout.CENTER);
        add(resumen, BorderLayout.SOUTH);

        actualizar.addActionListener(e -> recargar());
        aprobar.addActionListener(e -> aprobar());
        rechazar.addActionListener(e -> rechazar());
    }

    public final void recargar() {
        try {
            List<Institucion> lista = servicio.listarInstitucionesEnRevision();
            modelo.setFilas(lista);
            resumen.setText(lista.isEmpty()
                    ? "No hay instituciones por revisar."
                    : lista.size() + " institución(es) esperando revisión.");
        } catch (SQLException e) {
            Dialogos.mostrarError(this, e);
        }
    }

    private Institucion seleccionada() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            Dialogos.info(this, "Selecciona una institución de la tabla.");
            return null;
        }
        return modelo.getFila(tabla.convertRowIndexToModel(fila));
    }

    private String alumno(Institucion i) {
        return i.getCreadaPorNombre() == null ? "El alumno" : i.getCreadaPorNombre();
    }

    private void aprobar() {
        Institucion i = seleccionada();
        if (i == null || !Dialogos.confirmar(this, "¿Aprobar \"" + i.getNombre() + "\"?\n"
                + alumno(i) + " podrá empezar a registrar sus horas.")) {
            return;
        }
        try {
            servicio.aprobarInstitucion(i.getIdInstitucion());
            Dialogos.info(this, "Institución aprobada.");
        } catch (ReglaNegocioException | SQLException e) {
            Dialogos.mostrarError(this, e);
        }
        recargar();
    }

    private void rechazar() {
        Institucion i = seleccionada();
        if (i == null || !Dialogos.confirmar(this, "¿Rechazar \"" + i.getNombre() + "\"?\n"
                + alumno(i) + " tendrá que registrar otra institución.")) {
            return;
        }
        try {
            servicio.rechazarInstitucion(i.getIdInstitucion());
            Dialogos.info(this, "Institución rechazada.");
        } catch (ReglaNegocioException | SQLException e) {
            Dialogos.mostrarError(this, e);
        }
        recargar();
    }
}
