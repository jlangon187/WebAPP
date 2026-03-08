package com.gpbmods.backend.dto;

public class AuthResponse {
    private String token;
    private String rol;
    private String guid;
    private String nombre;

    public AuthResponse(String token, String rol, String guid, String nombre) {
        this.token = token;
        this.rol = rol;
        this.guid = guid;
        this.nombre = nombre;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public String getGuid() { return guid; }
    public void setGuid(String guid) { this.guid = guid; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
}
