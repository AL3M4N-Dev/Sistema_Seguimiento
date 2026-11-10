/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dao;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.mindrot.jbcrypt.BCrypt;
import util.Conexion;
/**
 * Recuperación de contraseña con un código de 8 caracteres.
 * En la BD (tabla tokens_recuperacion) solo se guarda el HASH SHA-256 del código,
 * nunca el código en claro. Cada código vale 15 minutos y se usa una sola vez.
 */
public class RecuperacionDAO {
    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int LONGITUD = 8;
    private static final int MINUTOS_VALIDEZ = 15;
    private static final SecureRandom RANDOM = new SecureRandom();
 
    /**
     * Crea un código para ese correo y lo devuelve en claro (para enviarlo por correo).
     * Devuelve null si no hay una cuenta activa con ese correo.
     */
    public String generarCodigo(String correo) throws SQLException {
        try (Connection con = Conexion.getConexion()) {
            Integer idUsuario = buscarId(con, correo);
            if (idUsuario == null) {
                return null;
            }
            // Invalidar códigos anteriores sin usar
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE tokens_recuperacion SET usado = 1 WHERE id_usuario = ? AND usado = 0")) {
                ps.setInt(1, idUsuario);
                ps.executeUpdate();
            }
            String codigo = nuevoCodigo();
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO tokens_recuperacion (id_usuario, token, expira) "
                  + "VALUES (?, ?, DATE_ADD(NOW(), INTERVAL ? MINUTE))")) {
                ps.setInt(1, idUsuario);
                ps.setString(2, sha256(codigo));
                ps.setInt(3, MINUTOS_VALIDEZ);
                ps.executeUpdate();
            }
            return codigo;
        }
    }
 
    /**
     * Cambia la contraseña si el código es válido (correcto, sin usar y no vencido).
     * Devuelve false si el correo o el código no coinciden.
     */
    public boolean restablecer(String correo, String codigo, String nuevaPassword) throws SQLException {
        String sqlToken =
            "SELECT t.id_token, t.id_usuario FROM tokens_recuperacion t "
          + "JOIN usuarios u ON u.id_usuario = t.id_usuario "
          + "WHERE u.correo = ? AND u.activo = 1 AND t.token = ? "
          + "AND t.usado = 0 AND t.expira > NOW()";
 
        try (Connection con = Conexion.getConexion()) {
            con.setAutoCommit(false);
            try {
                int idToken = 0;
                int idUsuario = 0;
                try (PreparedStatement ps = con.prepareStatement(sqlToken)) {
                    ps.setString(1, correo.trim().toLowerCase());
                    ps.setString(2, sha256(codigo.trim().toUpperCase()));
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            con.rollback();
                            return false;
                        }
                        idToken = rs.getInt(1);
                        idUsuario = rs.getInt(2);
                    }
                }
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE usuarios SET password_hash = ? WHERE id_usuario = ?")) {
                    ps.setString(1, BCrypt.hashpw(nuevaPassword, BCrypt.gensalt(12)));
                    ps.setInt(2, idUsuario);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE tokens_recuperacion SET usado = 1 WHERE id_token = ?")) {
                    ps.setInt(1, idToken);
                    ps.executeUpdate();
                }
                con.commit();
                return true;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }
 
    // ---------- auxiliares ----------
    private Integer buscarId(Connection con, String correo) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT id_usuario FROM usuarios WHERE correo = ? AND activo = 1")) {
            ps.setString(1, correo.trim().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : null;
            }
        }
    }
 
    private String nuevoCodigo() {
        StringBuilder sb = new StringBuilder(LONGITUD);
        for (int i = 0; i < LONGITUD; i++) {
            sb.append(ALFABETO.charAt(RANDOM.nextInt(ALFABETO.length())));
        }
        return sb.toString();
    }
 
    private String sha256(String texto) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(texto.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : h) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();   // 64 caracteres: cabe justo en CHAR(64)
        } catch (NoSuchAlgorithmException | java.io.UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}
