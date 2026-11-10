/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package util;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Envío de correos por SMTP. Los datos de la cuenta se leen del archivo
 * correo.properties (en la carpeta raíz del proyecto), que NO se sube a GitHub.
 * Lo pueden usar todos los módulos: CorreoUtil.enviar(destino, asunto, cuerpo).
 */

public class CorreoUtil {
    private static final String ARCHIVO = "correo.properties";
    private static Properties cargar() {
        Properties p = new Properties();
        try (InputStreamReader in = new InputStreamReader(
                new FileInputStream(ARCHIVO), StandardCharsets.UTF_8)) {
            p.load(in);
        } catch (IOException e) {
            // Sin archivo: el correo se considera "no configurado"
        }
        return p;
    }
 
    /** true si existe correo.properties con usuario y clave. */
    public static boolean configurado() {
        Properties p = cargar();
        return !p.getProperty("usuario", "").trim().isEmpty()
            && !p.getProperty("clave", "").trim().isEmpty();
    }
 
    public static void enviar(String destino, String asunto, String cuerpo)
            throws MessagingException {
        Properties cfg = cargar();
        final String usuario = cfg.getProperty("usuario", "").trim();
        final String clave = cfg.getProperty("clave", "").replace(" ", "").trim();
 
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", cfg.getProperty("host", "smtp.gmail.com").trim());
        props.put("mail.smtp.port", cfg.getProperty("puerto", "587").trim());
 
        Session sesion = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(usuario, clave);
            }
        });
 
        MimeMessage mensaje = new MimeMessage(sesion);
        mensaje.setFrom(new InternetAddress(usuario));
        mensaje.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destino));
        mensaje.setSubject(asunto, "UTF-8");
        mensaje.setText(cuerpo, "UTF-8");
        Transport.send(mensaje);
    }
}
