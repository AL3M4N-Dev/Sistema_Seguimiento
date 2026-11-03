package vista.alumno;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import modelo.EstadoSolicitud;
import modelo.ProgresoAlumno;
import modelo.SolicitudConstancia;
import servicio.ConstanciaAlumnoService;
import servicio.ReglaNegocioException;
import util.Sesion;
import vista.comun.Dialogos;
import vista.comun.Estilo;
import vista.comun.Formato;

/**
 * Sección "Mi constancia" del menú principal del alumno.
 *
 * Muestra el avance de horas aprobadas, el botón "Solicitar constancia" (se
 * desbloquea al 100%), en qué va la solicitud y, cuando el responsable sube el
 * PDF, el botón "Descargar constancia en PDF".
 *
 * Para ponerla en el menú principal:
 *     ConstanciaAlumnoPanel constancia = new ConstanciaAlumnoPanel();   // usa Sesion.getUsuario()
 *     panelDelMenu.add(constancia);
 *     constancia.recargar();   // llámalo otra vez cuando el menú se actualice
 */
public class ConstanciaAlumnoPanel extends JPanel {

    private final ConstanciaAlumnoService servicio;
    private final JLabel institucion = Estilo.secundario(" ");
    private final JLabel avance = new JLabel(" ");
    private final JProgressBar barra = new JProgressBar(0, 100);
    private final JLabel pendientes = Estilo.secundario(" ");
    private final JLabel estado = new JLabel(" ");
    private final JButton solicitar = Estilo.botonPrincipal("Solicitar constancia", Estilo.AZUL);
    private final JButton descargar = Estilo.botonPrincipal("Descargar constancia en PDF", Estilo.VERDE);

    /** Para el alumno que inició sesión (util.Sesion). */
    public ConstanciaAlumnoPanel() {
        this(new ConstanciaAlumnoService(Sesion.getUsuario()));
    }

    public ConstanciaAlumnoPanel(ConstanciaAlumnoService servicio) {
        this.servicio = servicio;
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel encabezado = new JPanel(new BorderLayout(0, 4));
        encabezado.setOpaque(false);
        encabezado.add(Estilo.subtitulo("Mi constancia de liberación"), BorderLayout.NORTH);
        encabezado.add(institucion, BorderLayout.SOUTH);

        barra.setForeground(Estilo.VERDE);
        barra.setPreferredSize(new Dimension(10, 12));
        estado.setOpaque(true);
        estado.setFont(estado.getFont().deriveFont(Font.BOLD));
        estado.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        JPanel centro = new JPanel(new GridBagLayout());
        centro.setOpaque(false);
        agregar(centro, avance, 0, 0);
        agregar(centro, barra, 1, 6);
        agregar(centro, pendientes, 2, 4);
        agregar(centro, estado, 3, 14);

        JButton actualizar = new JButton("Actualizar");
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botones.setOpaque(false);
        botones.add(actualizar);
        botones.add(descargar);
        botones.add(solicitar);

        add(encabezado, BorderLayout.NORTH);
        add(centro, BorderLayout.CENTER);
        add(botones, BorderLayout.SOUTH);

        actualizar.addActionListener(e -> recargar());
        solicitar.addActionListener(e -> solicitar());
        descargar.addActionListener(e -> descargar());
    }

    /** Vuelve a leer el avance y la solicitud desde la base. */
    public final void recargar() {
        try {
            ProgresoAlumno p = servicio.progreso();
            mostrar(p, servicio.solicitud(p));
        } catch (SQLException e) {
            Dialogos.mostrarError(this, e);
        }
    }

    private void mostrar(ProgresoAlumno p, SolicitudConstancia s) {
        if (p == null) {
            institucion.setText("Sin institución");
            avance.setText("Horas aprobadas: 0");
            barra.setValue(0);
            pendientes.setText(" ");
            mensaje("Aún no estás inscrito a una institución.", Estilo.FONDO, Estilo.TEXTO_SECUNDARIO);
            solicitar.setVisible(true);
            solicitar.setEnabled(false);
            solicitar.setText("Solicitar constancia");
            descargar.setVisible(false);
            return;
        }
        int porcentaje = Formato.porcentaje(p.getHorasAprobadas(), p.getHorasRequeridas());
        institucion.setText(p.getInstitucion());
        avance.setText("Horas aprobadas: " + Formato.horas(p.getHorasAprobadas()) + " de "
                + p.getHorasRequeridas() + " h (" + porcentaje + "%)");
        barra.setValue(porcentaje);
        BigDecimal enRevision = p.getHorasPendientes() == null ? BigDecimal.ZERO : p.getHorasPendientes();
        pendientes.setText(enRevision.signum() > 0
                ? Formato.horas(enRevision) + " h esperan la revisión del docente (aún no cuentan)."
                : " ");

        // Visible (bloqueado si no llega al 100%) mientras no haya solicitud o si se la rechazaron;
        // oculto cuando ya está en trámite o lista.
        boolean rechazada = s != null && s.getEstado() == EstadoSolicitud.RECHAZADA;
        solicitar.setVisible(s == null || rechazada);
        solicitar.setEnabled(ConstanciaAlumnoService.puedeSolicitar(p, s));
        solicitar.setText(rechazada ? "Volver a solicitar" : "Solicitar constancia");
        descargar.setVisible(s != null && s.getEstado() == EstadoSolicitud.DISPONIBLE);

        if (s == null) {
            if (p.completo()) {
                mensaje("¡Completaste tus horas! Ya puedes solicitar tu constancia.",
                        Estilo.APROBADO_FONDO, Estilo.APROBADO_TEXTO);
            } else {
                mensaje("Te faltan " + Formato.horas(p.horasFaltantes()) + " h aprobadas. El botón para "
                        + "solicitar tu constancia se desbloquea al llegar al 100%.",
                        Estilo.FONDO, Estilo.TEXTO_SECUNDARIO);
            }
            return;
        }
        switch (s.getEstado()) {
            case SOLICITADA -> mensaje("Solicitud enviada el " + Formato.fechaHora(s.getFechaSolicitud())
                    + ". El responsable de área está revisando tus horas.",
                    Estilo.PENDIENTE_FONDO, Estilo.PENDIENTE_TEXTO);
            case VALIDADA -> mensaje("Tus horas ya fueron validadas. Tu constancia estará lista pronto.",
                    Estilo.VALIDADA_FONDO, Estilo.VALIDADA_TEXTO);
            case DISPONIBLE -> mensaje("Tu constancia está lista. Descárgala en PDF.",
                    Estilo.APROBADO_FONDO, Estilo.APROBADO_TEXTO);
            case RECHAZADA -> mensaje("Tu solicitud fue rechazada"
                    + (s.getComentario() == null ? "." : ": " + s.getComentario())
                    + (p.completo() ? " Puedes volver a solicitarla." : ""),
                    Estilo.RECHAZADO_FONDO, Estilo.RECHAZADO_TEXTO);
        }
    }

    private void solicitar() {
        if (!Dialogos.confirmar(this, "¿Enviar la solicitud de tu constancia al responsable de área?")) {
            return;
        }
        try {
            servicio.solicitar();
            Dialogos.info(this, "Solicitud enviada. Te avisaremos aquí cuando tu constancia esté lista.");
        } catch (ReglaNegocioException | SQLException e) {
            Dialogos.mostrarError(this, e);
        }
        recargar();
    }

    private void descargar() {
        JFileChooser selector = new JFileChooser();
        selector.setDialogTitle("Guardar mi constancia");
        selector.setSelectedFile(new File("constancia_" + servicio.getAlumno().getNombre().replaceAll("\\W+", "_") + ".pdf"));
        if (selector.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            servicio.descargar(selector.getSelectedFile());
            Dialogos.info(this, "Constancia guardada en:\n" + selector.getSelectedFile().getAbsolutePath());
        } catch (ReglaNegocioException | SQLException | IOException e) {
            Dialogos.mostrarError(this, e);
        }
    }

    /** Mensaje de estado con colores; el texto se ajusta a varias líneas. */
    private void mensaje(String texto, Color fondo, Color letra) {
        String seguro = texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        estado.setText("<html><body style='width: 460px'>" + seguro + "</body></html>");
        estado.setBackground(fondo);
        estado.setForeground(letra);
    }

    private static void agregar(JPanel panel, JComponent c, int fila, int espacioArriba) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = fila;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.NORTHWEST;
        g.insets = new Insets(espacioArriba, 0, 0, 0);
        panel.add(c, g);
    }
}
