package vista.comun;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.basic.BasicFileChooserUI;

/**
 * Ventana para ELEGIR o GUARDAR un archivo, con el diseño base: se abre centrada sobre la
 * ventana del programa, con el mismo encabezado, botones y velo que las demás ventanas.
 * Recuerda la última carpeta que se usó mientras el programa está abierto.
 *
 *   File pdf = SelectorArchivo.abrir(this, "Elegir el PDF", "Solo PDF, máximo 10 MB", "Archivos PDF", "pdf");
 *   File destino = SelectorArchivo.guardar(this, "Guardar mi constancia", "Elige la carpeta",
 *           new File("constancia.pdf"), "Archivos PDF", "pdf");
 *   // las dos devuelven null si el usuario cancela
 */
public final class SelectorArchivo {

    private static final int ANCHO = 900;
    private static final int ALTO = 620;
    private static File ultimaCarpeta;

    private SelectorArchivo() {
    }

    /** Elige un archivo que ya existe. Devuelve null si el usuario cancela. */
    public static File abrir(Component padre, String titulo, String subtitulo,
                             String descripcionFiltro, String... extensiones) {
        return mostrar(padre, titulo, subtitulo, null, descripcionFiltro, extensiones, false, "Elegir archivo");
    }

    /**
     * Elige dónde guardar un archivo. Si ya existe, pregunta antes de reemplazarlo.
     * Si el usuario no escribe la extensión, se agrega sola. Devuelve null si cancela.
     */
    public static File guardar(Component padre, String titulo, String subtitulo, File sugerido,
                               String descripcionFiltro, String extension) {
        return mostrar(padre, titulo, subtitulo, sugerido, descripcionFiltro, new String[]{extension}, true, "Guardar aquí");
    }

    private static File mostrar(Component padre, String titulo, String subtitulo, File sugerido,
                                String descripcionFiltro, String[] extensiones, boolean guardar, String textoBoton) {
        JFileChooser selector = new JFileChooser(ultimaCarpeta);
        selector.setDialogType(guardar ? JFileChooser.SAVE_DIALOG : JFileChooser.OPEN_DIALOG);
        selector.setControlButtonsAreShown(false);      // los botones son los del diseño base
        if (descripcionFiltro != null && extensiones.length > 0) {
            selector.setFileFilter(new FileNameExtensionFilter(descripcionFiltro, extensiones));
            selector.setAcceptAllFileFilterUsed(false);
        }
        if (sugerido != null) {
            selector.setSelectedFile(ultimaCarpeta == null || sugerido.isAbsolute()
                    ? sugerido : new File(ultimaCarpeta, sugerido.getName()));
        }
        selector.setOpaque(false);

        VentanaDialogo v = new VentanaDialogo(padre, titulo, subtitulo);
        v.setSeparador(true);
        v.setFondoContenido(Estilo.FONDO);
        v.setMargenContenido(14, 18, 8, 18);
        v.setContenido(selector);
        v.setTamano(ANCHO, ALTO);

        File[] elegido = {null};
        v.agregarBotonCancelar("Cancelar");
        Boton aceptar = v.agregarBoton(Estilo.botonPrincipal(textoBoton));
        // Usa la acción del propio selector: así respeta lo que se escribió en "Nombre de archivo"
        // y, si se eligió una carpeta, entra en ella en lugar de cerrar.
        aceptar.addActionListener(e -> {
            if (selector.getUI() instanceof BasicFileChooserUI) {
                ((BasicFileChooserUI) selector.getUI()).getApproveSelectionAction()
                        .actionPerformed(new ActionEvent(selector, ActionEvent.ACTION_PERFORMED, "aprobar"));
            } else {
                selector.approveSelection();
            }
        });
        selector.addActionListener(e -> {
            if (JFileChooser.CANCEL_SELECTION.equals(e.getActionCommand())) {
                v.cerrar();
                return;
            }
            if (!JFileChooser.APPROVE_SELECTION.equals(e.getActionCommand())) {
                return;
            }
            File f = selector.getSelectedFile();
            if (f == null) {
                return;
            }
            if (guardar) {
                if (!f.getName().contains(".") && extensiones.length > 0) {
                    f = new File(f.getParentFile(), f.getName() + "." + extensiones[0]);
                }
                if (f.exists() && !Dialogos.confirmar(v, "¿Reemplazar el archivo?",
                        "Ya existe \"" + f.getName() + "\" en esa carpeta. Si continúas, se cambiará por el nuevo.",
                        "Reemplazar")) {
                    return;
                }
            } else if (!f.isFile()) {
                Dialogos.aviso(v, "No se encontró el archivo", "Revisa el nombre o elige el archivo de la lista.");
                return;
            }
            ultimaCarpeta = f.getParentFile();
            elegido[0] = f;
            v.cerrar();
        });
        v.setBotonPorDefecto(aceptar);
        v.setFocoInicial(selector);
        v.mostrar();
        return elegido[0];
    }
}
