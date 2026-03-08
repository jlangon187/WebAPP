package com.gpbmods.backend.dto;

public class ProfileUpdateRequest {
    private String nombre;
    private String password;
    private String email;
    private String guid;

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getGuid() { return guid; }
    public void setGuid(String guid) { this.guid = guid; }

}
