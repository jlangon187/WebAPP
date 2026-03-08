package com.gpbmods.backend.dto;

public class CompraRequest {

    private Long modId;
    private String metodoPago;

    public Long getModId() { return modId; }
    public void setModId(Long modId) { this.modId = modId; }

    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }
}
