package vista.docente;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import modelo.EstadoRegistro;
import modelo.Programa;
import modelo.RegistroHoras;
import servicio.PanelDocenteService;
import vista.comun.Dialogos;
import vista.comun.EstadoRenderer;
import vista.comun.Estilo;
import vista.comun.Formato;
import vista.comun.ListaTableModel;

/** Pestaña 1: bandeja de registros de horas para aprobar o rechazar. */
public class RegistrosPanel extends JPanel {

    private static final int COL_ESTADO = 7;

    private final PanelDocenteService servicio;
    private final JComboBox<EstadoRegistro> filtro = new JComboBox<>(EstadoRegistro.values());
    private final ListaTableModel<RegistroHoras> modelo = new ListaTableModel<>(
            new String[]{"Alumno", "Matrícula", "Programa", "Institución", "Fecha", "Horario", "Horas", "Estado"},
            (r, col) -> switch (col) {
                case 0 -> r.getAlumnoNombre();
                case 1 -> r.getMatricula();
                case 2 -> Programa.texto(r.getTipoPrograma());
                case 3 -> r.getInstitucionNombre();
                case 4 -> Formato.fecha(r.getFecha());
                case 5 -> Formato.horario(r.getHoraInicio(), r.getHoraFin());
                case 6 -> Formato.horas(r.getHorasCalculadas()) + " h";
                default -> r.getEstado();
            });
    private final JTable tabla = new JTable(modelo);
    private final JLabel resumen = Estilo.secundario(" ");

    public RegistrosPanel(PanelDocenteService servicio) {
        this.servicio = servicio;
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JButton actualizar = new JButton("Actualizar");
        JButton revisar = Estilo.botonPrincipal("Revisar registro", Estilo.AZUL);

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        acciones.add(new JLabel("Estado:"));
        acciones.add(filtro);
        acciones.add(actualizar);
        acciones.add(revisar);

        String alcance = servicio.getUsuario().esResponsable()
                ? "Registros de todos los alumnos"
                : "Registros de tus alumnos asignados (y de los que aún no tienen docente)";
        JPanel textos = new JPanel(new BorderLayout(0, 4));
        textos.add(Estilo.subtitulo("Registros de horas"), BorderLayout.NORTH);
        textos.add(Estilo.secundario(alcance), BorderLayout.SOUTH);

        JPanel arriba = new JPanel(new BorderLayout());
        arriba.add(textos, BorderLayout.WEST);
        arriba.add(acciones, BorderLayout.EAST);

        tabla.setRowHeight(32);
        tabla.setAutoCreateRowSorter(true);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.getColumnModel().getColumn(COL_ESTADO).setCellRenderer(new EstadoRenderer());
        int[] anchos = {170, 90, 110, 200, 90, 110, 60, 100};
        for (int i = 0; i < anchos.length; i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }

        add(arriba, BorderLayout.NORTH);
        add(new JScrollPane(tabla), BorderLayout.CENTER);
        add(resumen, BorderLayout.SOUTH);

        filtro.addActionListener(e -> recargar());
        actualizar.addActionListener(e -> recargar());
        revisar.addActionListener(e -> abrirDetalle());
        tabla.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    abrirDetalle();
                }
            }
        });
    }

    public final void recargar() {
        EstadoRegistro estado = (EstadoRegistro) filtro.getSelectedItem();
        try {
            List<RegistroHoras> lista = servicio.listarRegistros(estado);
            modelo.setFilas(lista);
            resumen.setText(lista.size() + " registro(s) con estado \"" + estado + "\"."
                    + (estado == EstadoRegistro.PENDIENTE ? " Doble clic en uno para revisarlo." : ""));
        } catch (SQLException e) {
            Dialogos.mostrarError(this, e);
        }
    }

    private void abrirDetalle() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            Dialogos.info(this, "Selecciona un registro de la tabla.");
            return;
        }
        RegistroHoras r = modelo.getFila(tabla.convertRowIndexToModel(fila));
        if (DetalleRegistroDialog.abrir(this, servicio, r.getIdRegistro())) {
            recargar();
        }
    }
}
