# Arquitetura do Sistema

## 1. Objetivo

O sistema processa tarefas de forma assincrona para desacoplar a recepcao HTTP da execucao de negocio.
A API responde rapido ao cliente e o processamento acontece no worker via fila.

## 2. Componentes

### 2.1 api-service

Responsabilidades:

- Autenticar clientes via JWT (`/api/v1/auth/token`).
- Expor endpoints REST em `/api/v1/tasks`.
- Validar payload de criacao.
- Aplicar rate limit na criacao de tarefas.
- Persistir a tarefa na tabela `tasks`.
- Publicar mensagem `TaskMessage` no RabbitMQ.

Principais classes:

- `TaskController`
- `AuthController`
- `TaskService`
- `TaskProducer`
- `SecurityConfig`
- `JwtAuthenticationFilter`
- `RabbitMQConfig`
- `GlobalExceptionHandler`

### 2.2 worker-service

Responsabilidades:

- Consumir mensagens da fila configurada em `task.messaging.queue`.
- Buscar a tarefa no banco.
- Executar regra de negocio por tipo da tarefa.
- Atualizar status/result/retryCount no banco.
- Reenfileirar quando houver falha recuperavel.

Principais classes:

- `TaskConsumer`
- `TaskWorkerService`
- `TaskExecutionService`
- `TaskRetryProducer`
- `RabbitMQConfig`

### 2.3 RabbitMQ

Configuracao atual:

- Exchange: `task.exchange` (topic)
- Queue: `task.queue` (durable)
- Routing key: `task.routing`

A API publica mensagens e o Worker consome da mesma fila.
No retry, o Worker republica a mensagem para o mesmo exchange/routing key.

### 2.4 PostgreSQL

Banco: `tasksdb`
Tabela principal: `tasks`

A tabela e compartilhada pela API e pelo Worker.

## 3. Fluxo ponta a ponta

```text
1) Cliente -> POST /api/v1/auth/token
2) API valida credenciais e retorna JWT
3) Cliente -> POST /api/v1/tasks com Authorization: Bearer <JWT>
4) API valida request + rate limit
5) API salva task com status=PENDING e retryCount=0
6) API publica TaskMessage no RabbitMQ
7) Worker consome TaskMessage
8) Worker marca task como PROCESSING
9) Worker executa logica
10a) Sucesso -> COMPLETED + result
10b) Falha -> retryCount++
     10b.1) retryCount < maxRetries: PENDING + reenfileira
     10b.2) retryCount >= maxRetries: FAILED
```

## 4. Modelo de dados

Tabela: `tasks`

- `id` (`UUID`, PK)
- `type` (`VARCHAR`, obrigatorio)
- `status` (`ENUM STRING`, obrigatorio)
- `payload` (`TEXT`)
- `result` (`TEXT`)
- `retry_count` (`INTEGER`, obrigatorio)
- `created_at` (`TIMESTAMP`, preenchido em `@PrePersist`)
- `updated_at` (`TIMESTAMP`, atualizado em `@PreUpdate`)

Observacoes:

- Em `dev`: `ddl-auto=update` e Flyway desabilitado.
- Em `prod`: `ddl-auto=validate` e Flyway habilitado.
- `retryCount` recebe `0` por padrao se vier `null` no persist.

## 5. Maquina de estados

Estados possiveis:

- `PENDING`
- `PROCESSING`
- `COMPLETED`
- `FAILED`

Transicoes:

- Criacao: `null -> PENDING`
- Inicio de execucao: `PENDING -> PROCESSING`
- Sucesso: `PROCESSING -> COMPLETED`
- Falha com retry disponivel: `PROCESSING -> PENDING`
- Falha definitiva: `PROCESSING -> FAILED`

## 6. Politica de retry

Configuracao:

- Propriedade: `task.max-retries`
- Valor padrao: `3`

Regra implementada:

- Em falha, calcula `nextRetry = retryCount + 1`.
- Se `nextRetry < maxRetries`, reenfileira.
- Se `nextRetry >= maxRetries`, finaliza como `FAILED`.

Com `maxRetries=3`, o comportamento pratico e:

- tentativa 1 falha -> retryCount=1 -> reenfileira
- tentativa 2 falha -> retryCount=2 -> reenfileira
- tentativa 3 falha -> retryCount=3 -> `FAILED`

## 7. Regras de execucao por tipo

Implementadas em `TaskExecutionService`:

- `REPORT`
- `EMAIL`
- `DATA_PROCESS`
- qualquer outro tipo cai no processamento generico

Regra especial para testes:

- Se o `payload` contiver `FORCE_ERROR` (case-insensitive), o Worker lanca excecao para simular falha.

## 8. Idempotencia e consistencia

Comportamentos atuais:

- Se a mensagem chegar para uma tarefa inexistente, o Worker apenas loga warning e ignora.
- Se a tarefa ja estiver `COMPLETED`, o Worker ignora nova tentativa.
- Nao existe lock distribudo explicito; o controle depende do fluxo de mensagens e das atualizacoes de status.

## 9. Controles de seguranca aplicados

- JWT Bearer para endpoints de tarefas.
- Credenciais externalizadas por variaveis de ambiente.
- Tratamento de erro 500 com mensagem generica (sem vazar detalhes internos).
- Rate limit no `POST /api/v1/tasks`.

## 10. Limites conhecidos

- Nao ha DLQ dedicada.
- Nao ha endpoint para cancelar tarefa em processamento.
- Nao ha suite de testes automatizados no repositorio atual.
