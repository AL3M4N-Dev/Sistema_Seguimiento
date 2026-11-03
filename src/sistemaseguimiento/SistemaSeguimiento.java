/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package sistemaseguimiento;

import dao.UsuarioDAO;
import java.sql.Connection;
import modelo.Usuario;
import util.Conexion;
import util.Sesion;
/**
 *
 * @author luisa
 */
public class SistemaSeguimiento {

    /**
     * @param args the command line arguments
     */
     public static void main(String[] args) {
        try (Connection con = Conexion.getConexion()) {
            System.out.println("Conectado a: " + con.getCatalog());
 
            UsuarioDAO dao = new UsuarioDAO();
            String correo = "prueba@escuela.edu";
 
            if (!dao.existeCorreo(correo)) {
                Usuario u = new Usuario();
                u.setNombre("Alumno Prueba");
                u.setMatricula("A0001");
                u.setCarrera("Ingeniería en Sistemas");
                u.setCorreo(correo);
                u.setTipoPrograma("servicio_social");
                dao.registrar(u, "clave1234");
                System.out.println("Usuario de prueba registrado");
            }
 
            Usuario logueado = dao.autenticar(correo, "clave1234");
            if (logueado != null) {
                Sesion.iniciar(logueado);
                System.out.println("Login OK: " + logueado.getNombre() + " (" + logueado.getRol() + ")");
                System.out.println("¿Ya inscrito?: " + dao.tieneInscripcion(logueado.getIdUsuario()));
            } else {
                System.out.println("Login fallido");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
