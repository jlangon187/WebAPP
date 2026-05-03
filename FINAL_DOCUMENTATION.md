# GPB Mods WebAPP - Documentacion Final

## Estado del proyecto

El proyecto queda preparado para despliegue en produccion con backend Spring Boot, frontend Angular, base de datos MySQL externa y worker de cifrado en PowerShell.

Se han completado los flujos de compra, cifrado por GUID, entrega por email con enlace temporal, gestion administrativa y panel de usuario.

## Arquitectura

- Frontend Angular servido por Nginx.
- Backend Spring Boot (API REST).
- MySQL externo (`gpbikes-db`) como base de datos principal.
- Worker externo (`worker/encryption-worker.ps1`) para procesar la cola de cifrado.
- Deploy con `docker-compose.prod.yml` en Portainer.

## Flujos funcionales cerrados

### 1) Compra y confirmacion

- Compra simulada disponible y operativa.
- Integracion Stripe y PayPal implementada con flujo de:
  - creacion de sesion/orden,
  - retorno al frontend,
  - confirmacion backend,
  - registro de compra.
- Envio de email de recibo al usuario y aviso de compra al admin.

### 2) Descarga cifrada por GUID

- Cada descarga genera un trabajo en cola (`EncryptionJob`).
- El worker toma el job, copia los archivos a carpeta temporal, cifra con `lock.exe` y empaqueta en `.rar`.
- El archivo final se mueve a la ruta de compras del usuario GUID.
- Se genera token temporal de descarga con expiracion.
- Se envia email automatico con enlace publico de descarga.

### 3) Entrega por enlace temporal

- Endpoint publico por token: `/api/descargas/file/{token}`.
- Se evita requerir login para descarga desde email.
- El frontend tambien soporta descarga autenticada por blob desde panel usuario.

### 4) Administracion

- Nuevo gestor de usuarios `/admin/users`:
  - busqueda,
  - edicion de nombre/email/GUID/rol/password,
  - activacion/desactivacion de cuenta,
  - visualizacion de compras.
- Reenvio manual de correo de descarga por compra.
- Overview de cola de cifrado (pending/running/done/failed).

### 5) Panel usuario y UX

- Panel con datos de cuenta y metricas.
- Preparacion de descarga con estados visibles (`En cola`, `Preparando enlace...`, `Enlace generado`).
- Boton de descarga protegido contra multi-click.
- Busqueda global desde header conectada al catalogo.

## Seguridad y validaciones

- GUID oficial unificado a 18 hexadecimales (`^[A-F0-9]{18}$`).
- Bloqueo de login y uso de JWT para cuentas desactivadas.
- Endpoint interno worker protegido por `X-Worker-Key`.
- Tokens de descarga con expiracion y limpieza programada.

## Operacion y mantenimiento

- `EncryptionJobMaintenanceService` implementa:
  - timeout de jobs en RUNNING,
  - limpieza de archivos expirados,
  - reintento de notificaciones no enviadas.
- Claim atomico de jobs para soportar multiples workers sin colision.

## Variables de entorno criticas

- `MODS_DOWNLOAD_PUBLIC_BASE_URL`
- `FRONTEND_URL`
- `MODS_ENCRYPTION_WORKER_API_KEY`
- `STRIPE_SECRET_KEY`
- `PAYPAL_CLIENT_ID`
- `PAYPAL_CLIENT_SECRET`
- `SPRING_MAIL_*`
- `PURCHASE_NOTIFY_ADMIN_EMAIL` (opcional)

## Ficheros clave

- `docker-compose.prod.yml`
- `backend/src/main/resources/application.properties`
- `backend/src/main/java/com/gpbmods/backend/controller/DescargasController.java`
- `backend/src/main/java/com/gpbmods/backend/controller/InternalEncryptionJobController.java`
- `backend/src/main/java/com/gpbmods/backend/controller/AdminController.java`
- `backend/src/main/java/com/gpbmods/backend/controller/PaymentController.java`
- `backend/src/main/java/com/gpbmods/backend/service/EncryptionJobMaintenanceService.java`
- `backend/src/main/java/com/gpbmods/backend/service/EmailService.java`
- `worker/encryption-worker.ps1`
- `frontend/src/app/components/user-dashboard/user-dashboard.component.ts`
- `frontend/src/app/components/admin-users-manager/admin-users-manager.component.ts`
- `frontend/src/app/components/checkout/checkout.component.ts`

## Checklist final de validacion

1. Compra simulada, Stripe y PayPal registran compra correctamente.
2. Se envian emails de recibo (usuario) y notificacion (admin).
3. La descarga cifrada termina en DONE y envia enlace funcional por email.
4. El enlace temporal descarga sin login y expira correctamente.
5. Reenvio manual de correo funciona desde `/admin/users`.
6. Panel usuario muestra estados de cola y evita envios duplicados.

## Cierre

El proyecto queda funcionalmente cerrado para esta fase, con flujo end-to-end de compra a descarga cifrada, controles administrativos, notificaciones y base operativa de produccion.
