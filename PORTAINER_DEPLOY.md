# Despliegue en Portainer (desde GitHub)

Esta guia asume que Portainer hace deploy directo desde el repositorio y que el SSL/HTTPS se termina fuera (Synology reverse proxy o Nginx externo).

## 1) Crear o editar la Stack

1. En Portainer: `Stacks` -> `Add stack`.
2. Selecciona `Repository`.
3. Pega la URL del repo y la rama.
4. `Compose path`: `docker-compose.yml`.

## 2) Variables de entorno en Portainer

En la seccion `Environment variables` define estas claves (todas obligatorias):

- `MARIADB_DATABASE`
- `MARIADB_USER`
- `MARIADB_PASSWORD`
- `MARIADB_ROOT_PASSWORD`
- `DISCORD_CLIENT_ID`
- `DISCORD_CLIENT_SECRET`
- `DISCORD_REDIRECT_URI`
- `FRONTEND_URL`
- `JWT_SECRET` (minimo 32 caracteres)
- `SPRING_MAIL_HOST`
- `SPRING_MAIL_PORT`
- `SPRING_MAIL_USERNAME`
- `SPRING_MAIL_PASSWORD`

Referencia: puedes usar `./.env.example` como plantilla de valores.

## 3) Discord OAuth (importante)

La URI de callback en Discord Developer Portal debe coincidir exactamente con:

- `DISCORD_REDIRECT_URI`

Ejemplo comun en produccion:

- `https://tu-dominio.com/api/auth/discord/callback`

## 4) Deploy

1. Pulsa `Deploy the stack`.
2. Espera a que los servicios `db`, `backend` y `frontend` queden en estado `running`.

## 5) Verificacion minima

Probar en navegador:

- `https://tu-dominio.com`
- `https://tu-dominio.com/api/mods/catalog`

Probar flujos:

1. Login/registro
2. Catalogo + detalle mod (imagen/video)
3. Carrito + checkout
4. Dashboard usuario
5. Dashboard admin (crear/editar mod, categoria, showroom 1-3)

## 6) Problemas tipicos

- **Error OAuth Discord**: callback mal configurado o distinta entre Portainer y Discord.
- **No conecta DB**: credenciales/host incorrectos en variables `MARIADB_*`.
- **JWT error al arrancar**: `JWT_SECRET` vacio o demasiado corto.
- **No llegan correos**: `SPRING_MAIL_*` incorrectas o bloqueo del proveedor SMTP.

## 7) Seguridad recomendada

- No subir nunca `.env` real al repo.
- Rotar secretos si alguna vez estuvieron expuestos.
- Mantener el repo privado no sustituye la rotacion de credenciales.
