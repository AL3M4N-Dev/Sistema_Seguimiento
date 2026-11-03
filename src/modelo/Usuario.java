/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package modelo;

/**
 *
 * @author luisa
 */
public class Usuario {
    private int idUsuario;
    private String nombre;
    private String matricula;
    private String carrera;
    private String correo;
    private String rol;            // alumno | docente | responsable
    private String tipoPrograma;   // servicio_social | practicas (solo alumnos)
    private boolean activo;
 
    public int getIdUsuario() { return idUsuario; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }
 
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
 
    public String getMatricula() { return matricula; }
    public void setMatricula(String matricula) { this.matricula = matricula; }
 
    public String getCarrera() { return carrera; }
    public void setCarrera(String carrera) { this.carrera = carrera; }
 
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
 
    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }
 
    public String getTipoPrograma() { return tipoPrograma; }
    public void setTipoPrograma(String tipoPrograma) { this.tipoPrograma = tipoPrograma; }
 
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
 
    public boolean esAlumno()      { return "alumno".equals(rol); }
    public boolean esDocente()     { return "docente".equals(rol); }
    public boolean esResponsable() { return "responsable".equals(rol); }
}
