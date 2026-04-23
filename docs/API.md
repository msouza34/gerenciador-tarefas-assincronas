# API Reference

## 1. Informacoes gerais

- Base URL local: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Content-Type esperado: `application/json`

## 2. Seguranca de acesso

### 2.1 Endpoints publicos

- `POST /api/v1/auth/token`
- `GET /swagger-ui.html`
- `GET /swagger-ui/**`
- `GET /v3/api-docs/**`

### 2.2 Endpoints protegidos

- `POST /api/v1/tasks`
- `GET /api/v1/tasks`
- `GET /api/v1/tasks/{id}`

Todos os endpoints de tarefas exigem:

```http
Authorization: Bearer <accessToken>
```

## 3. Autenticacao JWT

### 3.1 Gerar token

`POST /api/v1/auth/token`

Request:

```json
{
  "username": "api-user",
  "password": "sua-senha"
}
```

Response (`200 OK`):

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9....",
  "tokenType": "Bearer",
  "expiresInSeconds": 3600
}
```

Erros comuns:

- `400 Bad Request`: body invalido (campos vazios ou ausentes).
- `401 Unauthorized`: usuario/senha invalidos.

## 4. Endpoints de tarefas

Base path: `/api/v1/tasks`

### 4.1 Criar tarefa

`POST /api/v1/tasks`

Request body:

```json
{
  "type": "REPORT",
  "payload": "dados de entrada"
}
```

Validacoes:

- `type`: obrigatorio, maximo 50 caracteres, apenas `[A-Za-z0-9_- ]`.
- `payload`: obrigatorio, maximo 5000 caracteres.

Rate limit:

- Aplicado apenas na criacao de tarefas.
- Parametros:
- `API_RATE_LIMIT_CREATE_TASK_CAPACITY` (default `20`).
- `API_RATE_LIMIT_CREATE_TASK_WINDOW_SECONDS` (default `60`).
- Ao exceder: `429 Too Many Requests`.

Resposta de sucesso:

- `201 Created`
- Body: `TaskResponse`

### 4.2 Listar tarefas

`GET /api/v1/tasks`

Resposta:

- `200 OK`
- Body: array de `TaskResponse`

### 4.3 Buscar tarefa por ID

`GET /api/v1/tasks/{id}`

Resposta:

- `200 OK` com `TaskResponse`
- `404 Not Found` se nao existir

## 5. Modelos

### 5.1 TaskResponse

```json
{
  "id": "3e89d5b7-5063-4e62-98bf-6ba6f60f43a2",
  "type": "REPORT",
  "status": "PENDING",
  "payload": "dados de entrada",
  "result": null,
  "retryCount": 0,
  "createdAt": "2026-04-23T14:02:11.267",
  "updatedAt": "2026-04-23T14:02:11.267"
}
```

`status`: `PENDING | PROCESSING | COMPLETED | FAILED`

### 5.2 ErrorResponse

```json
{
  "message": "Erro interno do servidor. Tente novamente mais tarde.",
  "timestamp": "2026-04-23T14:13:02.901",
  "path": "uri=/api/v1/tasks"
}
```

## 6. Codigos HTTP mais comuns

- `200 OK`
- `201 Created`
- `400 Bad Request`
- `401 Unauthorized`
- `403 Forbidden`
- `404 Not Found`
- `429 Too Many Requests`
- `500 Internal Server Error`

## 7. Fluxo de uso rapido

1. Gerar token.
2. Chamar endpoint de tarefa com header `Authorization: Bearer <token>`.
3. Consultar status ate `COMPLETED` ou `FAILED`.
