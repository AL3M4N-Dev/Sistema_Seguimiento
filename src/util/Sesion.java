/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package util;

import modelo.Usuario;
/**
 * Guarda el usuario que inició sesión. Todos los módulos del equipo
 * obtienen al alumno/docente actual con Sesion.getUsuario().
 */
public class Sesion {
    private static Usuario usuarioActual;
 
    public static void iniciar(Usuario u) { usuarioActual = u; }
    public static void cerrar()           { usuarioActual = null; }
    public static Usuario getUsuario()    { return usuarioActual; }
    public static boolean hayUsuario()    { return usuarioActual != null; }
}
