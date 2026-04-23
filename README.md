# Gerenciador de Tarefas Assincronas

Backend distribuido em Java com processamento assincrono via RabbitMQ.
O sistema foi dividido em dois servicos Spring Boot:

- `api-service`: recebe requisicoes HTTP, persiste tarefas e publica mensagens.
- `worker-service`: consome mensagens da fila, executa o processamento e atualiza o status no banco.

## Sumario

- [Visao geral](#visao-geral)
- [Arquitetura rapida](#arquitetura-rapida)
- [Tecnologias](#tecnologias)
- [Estrutura do repositorio](#estrutura-do-repositorio)
- [Como executar](#como-executar)
- [Seguranca](#seguranca)
- [Fluxo de processamento](#fluxo-de-processamento)
- [Documentacao detalhada](#documentacao-detalhada)

## Visao geral

Objetivo do projeto:

- Receber solicitacoes de tarefas de forma sincrona pela API.
- Processar tarefas em background sem bloquear a requisicao HTTP.
- Garantir rastreabilidade por status no banco (`PENDING`, `PROCESSING`, `COMPLETED`, `FAILED`).
- Reprocessar automaticamente em caso de falha, ate o limite configurado.

## Arquitetura rapida

```text
Cliente HTTP
    |
    v
api-service (porta 8080)
    | 1) salva task no Postgres
    | 2) publica mensagem no RabbitMQ
    v
RabbitMQ (exchange + queue + routing key)
    |
    v
worker-service (consumer)
    | 3) le task no banco
    | 4) processa
    | 5) atualiza status/result
    v
PostgreSQL
```

Pontos importantes:

- API e Worker compartilham a mesma tabela `tasks`.
- O Worker e idempotente para mensagens duplicadas ja concluidas (ignora quando status ja esta `COMPLETED`).
- Retry automatico usa a mesma fila (`task.queue`) e o mesmo exchange/routing key.

## Tecnologias

- Java 17
- Spring Boot 3.3.5
- Spring Web (API)
- Spring Validation (API)
- Spring Security + JWT
- Spring Data JPA
- Spring AMQP (RabbitMQ)
- Flyway (migracoes em `prod`)
- PostgreSQL 16
- RabbitMQ 3.13 (com Management Plugin)
- Springdoc OpenAPI / Swagger UI
- Lombok
- Docker e Docker Compose
- Maven

## Estrutura do repositorio

```text
.
|-- api-service/
|   |-- src/main/java/... (controller, service, entity, config, messaging)
|   |-- src/main/resources/application.yml
|   `-- Dockerfile
|-- worker-service/
|   |-- src/main/java/... (consumer, worker service, execution, entity, config)
|   |-- src/main/resources/application.yml
|   `-- Dockerfile
|-- docs/
|   |-- API.md
|   |-- ARQUITETURA.md
|   `-- OPERACAO.md
`-- docker-compose.yml
```

## Como executar

### Opcao 1 - Docker Compose (recomendado)

Antes de subir, crie o arquivo `.env` com os segredos:

```bash
# Linux/macOS
cp .env.example .env

# Windows PowerShell
Copy-Item .env.example .env
```

Depois ajuste os valores sensiveis no `.env` (senhas e segredo JWT).

Subir ambiente completo:

```bash
docker compose up --build
```

Subir em background:

```bash
docker compose up --build -d
```

Parar ambiente:

```bash
docker compose down
```

Servicos expostos:

- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- RabbitMQ Management: `http://localhost:15672` (credenciais via `.env`)
- PostgreSQL: `localhost:5432` (credenciais via `.env`)

### Opcao 2 - Execucao local (sem containers para API/Worker)

1. Suba apenas dependencias de infraestrutura (`postgres` e `rabbitmq`) com Docker.
2. Rode cada servico com Maven em terminais separados:

```bash
# terminal 1
cd api-service
mvn spring-boot:run

# terminal 2
cd worker-service
mvn spring-boot:run
```

## Seguranca

Implementacoes aplicadas:

- Autenticacao JWT com endpoint `POST /api/v1/auth/token`.
- Endpoints de tarefas protegidos por token Bearer.
- Rate limit para `POST /api/v1/tasks` (configuravel por ambiente).
- Perfil `prod` com `ddl-auto=validate` e Flyway habilitado.
- Segredos externalizados em variaveis de ambiente (sem hardcode).

Fluxo basico:

1. Gerar token:

```bash
curl -X POST "http://localhost:8080/api/v1/auth/token" \
  -H "Content-Type: application/json" \
  -d '{"username":"api-user","password":"sua-senha"}'
```

2. Chamar endpoint protegido:

```bash
curl -X POST "http://localhost:8080/api/v1/tasks" \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"type":"REPORT","payload":"dados"}'
```

Observacoes:

- `/api/v1/auth/token`, Swagger e `/v3/api-docs` sao publicos.
- `/api/v1/tasks/**` exige JWT Bearer valido.
- Limite de criacao de tarefas (rate limit) e configuravel por ambiente.

## Fluxo de processamento

1. Cliente autentica em `POST /api/v1/auth/token`.
2. Cliente envia `POST /api/v1/tasks` com JWT Bearer.
3. API valida payload, salva tarefa com status `PENDING` e `retryCount=0`.
4. API publica mensagem `TaskMessage` no RabbitMQ.
5. Worker consome mensagem e marca tarefa como `PROCESSING`.
6. Worker tenta executar a tarefa:
- sucesso: grava `COMPLETED` e o `result`.
- falha: incrementa `retryCount`.
- se `retryCount < task.max-retries` (default `3`), volta para `PENDING` e reenfileira.
- se `retryCount >= task.max-retries`, marca `FAILED`.

Teste de falha controlada:

- Envie um `payload` contendo `FORCE_ERROR`.
- O Worker vai gerar erro intencional e ativar a politica de retry.

## Documentacao detalhada

- [API detalhada](docs/API.md)
- [Arquitetura e ciclo de vida](docs/ARQUITETURA.md)
- [Guia de operacao e troubleshooting](docs/OPERACAO.md)
