package com.gpbmods.backend.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "mods")
public class Mods {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(nullable = false, length = 50)
    private String version;

    @Column(name = "archivo_original", nullable = false)
    private String archivoOriginal; // Relates to NAS path or reference

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @Column(name = "destacado_home", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean destacadoHome = false;

    @Column(name = "orden_showroom")
    private Integer ordenShowroom;

    @Column(name = "youtube_url", length = 255)
    private String youtubeUrl;

    @Column(name = "creado_en", updatable = false)
    @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY)
    private LocalDateTime creadoEn = LocalDateTime.now();

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getArchivoOriginal() { return archivoOriginal; }
    public void setArchivoOriginal(String archivoOriginal) { this.archivoOriginal = archivoOriginal; }

    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }

    public boolean isDestacadoHome() { return destacadoHome; }
    public void setDestacadoHome(boolean destacadoHome) { this.destacadoHome = destacadoHome; }

    public Integer getOrdenShowroom() { return ordenShowroom; }
    public void setOrdenShowroom(Integer ordenShowroom) { this.ordenShowroom = ordenShowroom; }

    public String getYoutubeUrl() { return youtubeUrl; }
    public void setYoutubeUrl(String youtubeUrl) { this.youtubeUrl = youtubeUrl; }

    public LocalDateTime getCreadoEn() { return creadoEn; }
    public void setCreadoEn(LocalDateTime creadoEn) { this.creadoEn = creadoEn; }
}
