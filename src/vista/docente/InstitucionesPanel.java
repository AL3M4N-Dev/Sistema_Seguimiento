package vista.docente;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.sql.SQLException;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import modelo.Institucion;
import servicio.PanelDocenteService;
import servicio.ReglaNegocioException;
import vista.comun.Boton;
import vista.comun.Dialogos;
import vista.comun.Estilo;
import vista.comun.Formato;
import vista.comun.ListaTableModel;
import vista.comun.Tarjeta;

/** Sección "Instituciones": las de prácticas que dieron de alta los alumnos. */
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
        setLayout(new BorderLayout(0, 16));
        setBackground(Estilo.FONDO);
        setBorder(BorderFactory.createEmptyBorder(22, 28, 22, 28));

        JPanel textos = Estilo.panel(new BorderLayout(0, 4));
        textos.add(Estilo.titulo("Instituciones de prácticas por aprobar"), BorderLayout.NORTH);
        textos.add(Estilo.secundario("Las dan de alta los alumnos. Mientras estén en revisión, el alumno no puede registrar horas."),
                BorderLayout.SOUTH);

        Boton actualizar = Estilo.botonSecundario("Actualizar");
        Boton rechazar = Estilo.botonPeligro("Rechazar");
        Boton aprobar = Estilo.botonExito("Aprobar");
        JPanel acciones = Estilo.panel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        acciones.add(actualizar);
        acciones.add(rechazar);
        acciones.add(aprobar);

        JPanel arriba = Estilo.panel(new BorderLayout(16, 0));
        arriba.add(textos, BorderLayout.CENTER);
        arriba.add(acciones, BorderLayout.EAST);

        Estilo.estilizarTabla(tabla);
        tabla.setAutoCreateRowSorter(true);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        int[] anchos = {210, 150, 200, 210, 70, 140};
        for (int i = 0; i < anchos.length; i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }
        Tarjeta marco = new Tarjeta(new BorderLayout(), Estilo.SUPERFICIE, Estilo.BORDE, 14);
        marco.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        marco.add(Estilo.scroll(tabla), BorderLayout.CENTER);

        add(arriba, BorderLayout.NORTH);
        add(marco, BorderLayout.CENTER);
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
            Dialogos.info(this, "Ninguna institución seleccionada", "Haz clic en una institución de la tabla.");
            return null;
        }
        return modelo.getFila(tabla.convertRowIndexToModel(fila));
    }

    private String alumno(Institucion i) {
        return i.getCreadaPorNombre() == null ? "El alumno" : i.getCreadaPorNombre();
    }

    private void aprobar() {
        Institucion i = seleccionada();
        if (i == null || !Dialogos.confirmarAprobar(this, "¿Aprobar esta institución?",
                alumno(i) + " podrá empezar a registrar sus horas en ella.", i.getNombre(), "Aprobar")) {
            return;
        }
        try {
            servicio.aprobarInstitucion(i.getIdInstitucion());
            Dialogos.exito(this, "Institución aprobada", "\"" + i.getNombre() + "\" ya está disponible para " + alumno(i) + ".");
        } catch (ReglaNegocioException | SQLException e) {
            Dialogos.mostrarError(this, e);
        }
        recargar();
    }

    private void rechazar() {
        Institucion i = seleccionada();
        if (i == null || !Dialogos.confirmarRechazo(this, "¿Rechazar esta institución?",
                "\"" + i.getNombre() + "\" quedará rechazada y " + alumno(i) + " tendrá que registrar otra.",
                "Rechazar")) {
            return;
        }
        try {
            servicio.rechazarInstitucion(i.getIdInstitucion());
            Dialogos.exito(this, "Institución rechazada", alumno(i) + " tendrá que registrar otra institución.");
        } catch (ReglaNegocioException | SQLException e) {
            Dialogos.mostrarError(this, e);
        }
        recargar();
    }
}
