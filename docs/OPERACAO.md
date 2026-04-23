# Guia de Operacao

## 1. Requisitos

- Java 17
- Maven 3.9+
- Docker e Docker Compose

## 2. Perfis de ambiente

- `dev`: defaults locais, `ddl-auto=update`, Flyway desabilitado.
- `prod`: sem defaults sensiveis, `ddl-auto=validate`, Flyway habilitado.

## 3. Subindo com Docker Compose

1. Criar `.env`:

```bash
# Linux/macOS
cp .env.example .env

# Windows PowerShell
Copy-Item .env.example .env
```

2. Ajustar segredos no `.env`:

- `POSTGRES_PASSWORD`
- `RABBITMQ_USER`
- `RABBITMQ_PASSWORD`
- `API_AUTH_USERNAME`
- `API_AUTH_PASSWORD`
- `API_JWT_SECRET`

3. Subir:

```bash
docker compose up --build
```

Comandos uteis:

```bash
docker compose logs -f
docker compose down
docker compose down -v
```

## 4. Variaveis principais

Banco e fila:

- `POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`
- `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USER`, `RABBITMQ_PASSWORD`

Seguranca API:

- `API_AUTH_USERNAME`
- `API_AUTH_PASSWORD`
- `API_JWT_SECRET` (minimo 32 caracteres recomendado)
- `API_JWT_EXPIRATION_SECONDS` (default: `3600`)

Rate limit:

- `API_RATE_LIMIT_CREATE_TASK_CAPACITY` (default: `20`)
- `API_RATE_LIMIT_CREATE_TASK_WINDOW_SECONDS` (default: `60`)

Worker:

- `TASK_MAX_RETRIES` (default: `3`)
- `TASK_PROCESSING_SIMULATED_DELAY_MS` (default: `2000`)

## 5. Migracao de banco

No perfil `prod`, os dois servicos executam Flyway com scripts em:

- `api-service/src/main/resources/db/migration`
- `worker-service/src/main/resources/db/migration`

Script inicial:

- `V1__create_tasks_table.sql`

## 6. Smoke test apos subir

1. Gerar token:

```bash
curl -X POST "http://localhost:8080/api/v1/auth/token" \
  -H "Content-Type: application/json" \
  -d '{"username":"<API_AUTH_USERNAME>","password":"<API_AUTH_PASSWORD>"}'
```

2. Criar tarefa com token:

```bash
curl -X POST "http://localhost:8080/api/v1/tasks" \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"type":"REPORT","payload":"teste de smoke"}'
```

3. Consultar tarefas:

```bash
curl -H "Authorization: Bearer <TOKEN>" "http://localhost:8080/api/v1/tasks"
```

## 7. Troubleshooting rapido

`401 Unauthorized`:

- token ausente/invalido ou expirado

`403 Forbidden`:

- token valido sem permissao (cenario futuro, caso evolua autorizacao por papeis)

`429 Too Many Requests`:

- limite de criacao de tarefas excedido

API sem subir em `prod`:

- variaveis obrigatorias faltando no ambiente

Falha de migracao:

- verificar tabela `flyway_schema_history` e permissao no banco
