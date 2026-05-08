# ShopRPA Server Quick Start

## Quick Start

1. Copy the environment file.

   ```powershell
   cp .env.example .env
   ```

   Before production use, replace `INTERNAL_ADMIN_API_KEY` and `REGISTER_BEARER_TOKEN` in `.env`.
   They protect internal service-to-service admin calls and registration bridge calls.

2. Validate the Compose file.

   ```powershell
   docker compose --env-file .env -f docker-compose.yml config --quiet
   ```

3. Start all services.

   ```powershell
   docker compose up -d
   ```

4. Check service status.

   ```powershell
   docker compose ps
   ```

## Access

The public entry point is the OpenResty gateway:

```text
http://127.0.0.1:32742
```

Most backend services are intentionally kept on the internal Docker network. Expose individual service ports only when debugging locally.

| Service | Internal port | Default external access |
| --- | ---: | --- |
| openresty-nginx | 80 | `http://127.0.0.1:32742` |
| casdoor | 8000 | `http://127.0.0.1:8000` |
| ai-service | 8010 | Internal only |
| openapi-service | 8020 | Internal only |
| resource-service | 8030 | Internal only |
| robot-service | 8040 | Internal only |
| rpa-auth | 10251 | Internal only |
| mysql | 3306 | Internal only |
| redis | 6379 | Internal only |
| minio | 9000/9001 | Internal only |

## Stop Services

```powershell
docker compose stop
```

## Common Commands

```powershell
# View logs
docker compose logs -f [service-name]

# Restart a service
docker compose restart [service-name]

# Rebuild and start
docker compose up --build -d

# Stop and remove volumes
docker compose down -v

# Check service status
docker compose ps
```

## Troubleshooting

1. Port conflicts: change published ports in `docker-compose.yml`.
2. Docker API permission errors: start Docker Desktop and make sure the current Windows user can access the Docker engine.
3. Service startup failures: inspect `docker compose logs [service-name]`.
4. Database startup: wait for MySQL health checks before debugging dependent services.
