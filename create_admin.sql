-- Script para crear el usuario administrador inicial
-- Credenciales:
-- Email: admin@gpbikes-mods.com
-- Contraseña: admin

INSERT INTO usuario (nombre, email, password_hash, guid, rol, creado_en)
VALUES ('Administrador', 'admin@gpbikes-mods.com', '$2a$10$n2GofB/02P5Jvj2/f51Yt.6yW3Y2x8UofqU2tD7Hh7O.m1Jd2WqQy', UUID(), 'admin', NOW());
