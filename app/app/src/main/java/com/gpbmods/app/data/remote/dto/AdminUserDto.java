package com.gpbmods.app.data.remote.dto;

import java.util.List;

public class AdminUserDto {
    public long id;
    public String nombre;
    public String email;
    public String guid;
    public boolean guidValid;
    public boolean profileCompleted;
    public String rol;
    public boolean activo;
    public String creadoEn;
    public int purchasesCount;
    public int ticketsCount;
    public double totalSpent;
    public List<PurchaseDto> purchases;

    public static class PurchaseDto {
        public long id;
        public String fecha;
        public double precioPagado;
        public String metodoPago;
        public String guidCompra;
        public ModResumen mod;
    }

    public static class ModResumen {
        public long id;
        public String nombre;
        public String version;
        public String archivoOriginal;
    }
}
