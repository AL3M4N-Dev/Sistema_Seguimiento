/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
/**
 *
 * @author luisa
 */
public class Conexion {
    private static final String URL =
        "jdbc:mysql://localhost:3306/servicio_social?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASS = "";   // XAMPP por defecto no tiene contraseña
 
    public static Connection getConexion() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}
