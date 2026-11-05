/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import modelo.Usuario;
import org.mindrot.jbcrypt.BCrypt;
import util.Conexion;
/**
 *
 * @author luisa
 */
public class UsuarioDAO {
    /** Registra un alumno. La contraseña se guarda hasheada con BCrypt. */
    public boolean registrar(Usuario u, String passwordPlano) throws SQLException {
        String sql = "INSERT INTO usuarios (nombre, matricula, carrera, correo, password_hash, rol, tipo_programa) "
           + "VALUES (?, ?, ?, ?, ?, ?, ?)";
    try (Connection con = Conexion.getConexion();
        PreparedStatement ps = con.prepareStatement(sql)) {
        ps.setString(1, u.getNombre());
        ps.setString(2, vacioANull(u.getMatricula()));
        ps.setString(3, u.getCarrera());
        ps.setString(4, u.getCorreo().trim().toLowerCase());
        ps.setString(5, BCrypt.hashpw(passwordPlano, BCrypt.gensalt(12)));
        ps.setString(6, u.getRol());
        ps.setString(7, u.getTipoPrograma());
        return ps.executeUpdate() == 1;
    }
    
    }
    /** Devuelve el usuario si correo y contraseña son correctos; si no, null. */
    public Usuario autenticar(String correo, String passwordPlano) throws SQLException {
        String sql = "SELECT * FROM usuarios WHERE correo = ? AND activo = 1";
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, correo.trim().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && BCrypt.checkpw(passwordPlano, rs.getString("password_hash"))) {
                    return mapear(rs);
                }
            }
        }
        return null;
    }
 
    public boolean existeCorreo(String correo) throws SQLException {
        return existe("SELECT 1 FROM usuarios WHERE correo = ?", correo.trim().toLowerCase());
    }
 
    public boolean existeMatricula(String matricula) throws SQLException {
        return existe("SELECT 1 FROM usuarios WHERE matricula = ?", matricula.trim());
    }
 
    /** true si el alumno ya se inscribió a una institución (si no, es su primera vez). */
    public boolean tieneInscripcion(int idAlumno) throws SQLException {
        String sql = "SELECT 1 FROM inscripciones WHERE id_alumno = ?";
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idAlumno);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
 
    // ---------- auxiliares ----------
    private boolean existe(String sql, String valor) throws SQLException {
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, valor);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
 
    private Usuario mapear(ResultSet rs) throws SQLException {
        Usuario u = new Usuario();
        u.setIdUsuario(rs.getInt("id_usuario"));
        u.setNombre(rs.getString("nombre"));
        u.setMatricula(rs.getString("matricula"));
        u.setCarrera(rs.getString("carrera"));
        u.setCorreo(rs.getString("correo"));
        u.setRol(rs.getString("rol"));
        u.setTipoPrograma(rs.getString("tipo_programa"));
        u.setActivo(rs.getBoolean("activo"));
        return u;
    }
 
    private String vacioANull(String s) {
        return (s == null || s.trim().isEmpty()) ? null : s.trim();
    }
}
