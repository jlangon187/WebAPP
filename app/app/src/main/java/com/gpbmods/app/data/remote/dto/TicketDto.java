package com.gpbmods.app.data.remote.dto;

public class TicketDto {
    public long id;
    public String estado;
    public String mensaje;
    public String creadoEn;
    public UsuarioResumen usuario;

    public static class UsuarioResumen {
        public long id;
        public String nombre;
        public String email;
    }
}
