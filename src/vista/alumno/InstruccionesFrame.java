package vista.alumno;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import modelo.Programa;
import modelo.Usuario;
import util.Sesion;
import vista.comun.Boton;
import vista.comun.Estilo;
import vista.comun.Tarjeta;

public class InstruccionesFrame extends JFrame {

    private final boolean esPracticas;

    public InstruccionesFrame() {
        Usuario alumno = Sesion.getUsuario();
        esPracticas = "practicas".equals(alumno.getTipoPrograma());

        setTitle("Instrucciones para inscribirte");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel raiz = new JPanel(new BorderLayout(0, 16));
        raiz.setBackground(Estilo.FONDO);
        raiz.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        JPanel encabezado = Estilo.panel(new BorderLayout(0, 4));
        encabezado.add(Estilo.titulo("¡Bienvenido, " + alumno.getNombre() + "!"), BorderLayout.NORTH);
        encabezado.add(Estilo.secundario("Tu programa: " + Programa.texto(alumno.getTipoPrograma())),
                BorderLayout.SOUTH);
        raiz.add(encabezado, BorderLayout.NORTH);

        Tarjeta tarjeta = new Tarjeta();
        tarjeta.add(Estilo.subtitulo("Cómo inscribirte"), BorderLayout.NORTH);

        JPanel pasos = Estilo.panel(null);
        pasos.setLayout(new BoxLayout(pasos, BoxLayout.Y_AXIS));
        if (esPracticas) {
            pasos.add(paso("1. Registra la institución donde harás tus prácticas: nombre, dirección, contacto y horas."));
            pasos.add(paso("2. Un docente revisará tu institución. Mientras esté \"en revisión\" no podrás registrar horas."));
            pasos.add(paso("3. Cuando la aprueben, registra tus horas desde el menú principal."));
        } else {
            pasos.add(paso("1. Revisa el catálogo de instituciones: verás los cupos disponibles y las horas requeridas."));
            pasos.add(paso("2. Elige una institución con cupo y pulsa \"Inscribirme\"."));
            pasos.add(paso("3. Solo puedes inscribirte una vez, así que elige con cuidado."));
            pasos.add(paso("4. Después registra tus horas desde el menú principal."));
        }
        tarjeta.add(pasos, BorderLayout.CENTER);
        raiz.add(tarjeta, BorderLayout.CENTER);

        JPanel botones = Estilo.panel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        Boton btnContinuar = Estilo.botonPrincipal(
                esPracticas ? "Registrar mi institución" : "Ver instituciones");
        botones.add(btnContinuar);
        raiz.add(botones, BorderLayout.SOUTH);

        btnContinuar.addActionListener(e -> continuar());
        getRootPane().setDefaultButton(btnContinuar);

        setContentPane(raiz);
        setSize(700, 520);
        setLocationRelativeTo(null);
    }

    // Una línea de instrucciones con espacio abajo.
    private static JPanel paso(String texto) {
        JPanel p = Estilo.panel(new BorderLayout());
        p.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
        p.add(Estilo.etiqueta("<html><body style='width:520px'>" + texto + "</body></html>",
                Estilo.fuente(java.awt.Font.PLAIN, 14), Estilo.TEXTO), BorderLayout.CENTER);
        return p;
    }

    // Abre la pantalla que corresponde al programa del alumno.
    private void continuar() {
        if (esPracticas) {
            new AltaInstitucionFrame().setVisible(true);
        } else {
            new CatalogoFrame().setVisible(true);
        }
        dispose();
    }
}
