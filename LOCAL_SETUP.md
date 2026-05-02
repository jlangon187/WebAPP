# Local Docker Setup

## 1) Prepare local env file

From `WebAPP/`, create your local `.env` from template:

```powershell
Copy-Item .env.local.example .env
```

Then edit `.env` and set your real values for:

- `DISCORD_CLIENT_ID`
- `DISCORD_CLIENT_SECRET`
- `JWT_SECRET`
- `SPRING_MAIL_USERNAME`
- `SPRING_MAIL_PASSWORD`

## 2) Start local stack

```powershell
./run-local.ps1
```

This starts:

- `db` (MariaDB)
- `backend` (Spring Boot)
- `frontend` (Nginx + Angular build)

## 3) Verify

- Frontend: `http://localhost:4200`
- API catalog: `http://localhost:8080/api/mods/catalog`
- Status: `docker compose ps`

## 4) Logs (if needed)

```powershell
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f db
```

## 5) Stop local stack

```powershell
./stop-local.ps1
```

## 6) Discord callback note

In Discord Developer Portal, include BOTH callbacks:

- `http://localhost:8080/api/auth/discord/callback`
- `https://javiliyors.myds.me/api/auth/discord/callback`
