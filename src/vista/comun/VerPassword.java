package vista.comun;

import javax.swing.JCheckBox;
import javax.swing.JPasswordField;

/**
 * Crea un checkbox "Mostrar contraseña" que muestra u oculta el texto de
 * uno o varios JPasswordField a la vez.
 *
 * Uso: panel.add(VerPassword.crear(txtPassword, txtConfirmar));
 */
public class VerPassword {

    public static JCheckBox crear(JPasswordField... campos) {
        final char ocultar = campos[0].getEchoChar();   // el carácter con que se oculta (•)
        final JCheckBox chk = new JCheckBox("Mostrar contraseña");
        chk.setFocusPainted(false);
        chk.addActionListener(e -> {
            char eco = chk.isSelected() ? (char) 0 : ocultar;   // (char) 0 = mostrar el texto
            for (JPasswordField campo : campos) {
                campo.setEchoChar(eco);
            }
        });
        return chk;
    }
}