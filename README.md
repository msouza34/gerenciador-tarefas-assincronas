# 🚀 Gerenciador de Tarefas Assincronas

**Backend distribuído em Java com processamento assincronico via RabbitMQ**

O sistema foi dividido em dois serviços Spring Boot independentes que trabalham em conjunto para processar tarefas de forma assincronica e escalável:

- **`api-service`**: Recebe requisições HTTP, persiste tarefas no banco de dados e publica mensagens na fila
- **`worker-service`**: Consome mensagens da fila, executa o processamento e atualiza o status no banco

---

## 📋 Sumário

- [Visão Geral](#visão-geral)
- [Arquitetura Rápida](#arquitetura-rápida)
- [Stack Tecnológico](#stack-tecnológico)
- [Estrutura do Repositório](#estrutura-do-repositório)
- [Como Executar](#como-executar)
- [Segurança](#segurança)
- [Fluxo de Processamento](#fluxo-de-processamento)
- [Documentação Detalhada](#documentação-detalhada)

---

## 🎯 Visão Geral

### Objetivos do Projeto

O sistema foi projetado para atender aos seguintes requisitos:

| Objetivo | Descrição |
|----------|-----------|
| **Recepção Síncrona** | Receber solicitações de tarefas de forma síncrona pela API REST |
| **Processamento Assincronico** | Processar tarefas em background sem bloquear a requisição HTTP |
| **Rastreabilidade** | Garantir rastreabilidade completa por status no banco de dados |
| **Resiliência** | Reprocessar automaticamente em caso de falha, até o limite configurado |

### Estados de Tarefa

As tarefas transitam pelos seguintes estados:

- **`PENDING`** — Tarefa aguardando processamento
- **`PROCESSING`** — Tarefa sendo processada pelo worker
- **`COMPLETED`** — Tarefa concluída com sucesso
- **`FAILED`** — Tarefa falhou após todas as tentativas de retry

---

## 🏗️ Arquitetura Rápida

### Fluxo de Dados

```
┌─────────────┐
│ Cliente HTTP│
└──────┬──────┘
       │
       ▼
┌──────────────────────────────────┐
│   api-service (porta 8080)       │
│ ┌──────────────────────────────┐ │
│ │ 1) Valida requisição         │ │
│ │ 2) Salva task no PostgreSQL  │ │
│ │ 3) Publica mensagem RabbitMQ │ │
│ └──────────────────────────────┘ │
└──────┬───────────────────────────┘
       │
       ▼
┌──────────────────────────────────┐
│  RabbitMQ                        │
│  (exchange + queue + routing key)│
└──────┬───────────────────────────┘
       │
       ▼
┌──────────────────────────────────┐
│   worker-service (consumer)      │
│ ┌──────────────────────────────┐ │
│ │ 3) Lê task no banco          │ │
│ │ 4) Processa tarefa           │ │
│ │ 5) Atualiza status/resultado │ │
│ └──────────────────────────────┘ │
└──────┬───────────────────────────┘
       │
       ▼
┌──────────────────────────────────┐
│      PostgreSQL (banco)          │
│  (tabela compartilhada: tasks)   │
└──────────────────────────────────┘
```

### Pontos Importantes da Arquitetura

| Aspecto | Descrição |
|---------|-----------|
| **Compartilhamento de Dados** | API e Worker compartilham a mesma tabela `tasks` no PostgreSQL |
| **Idempotência** | Worker é idempotente para mensagens duplicadas já concluídas (ignora quando status já está `COMPLETED`) |
| **Retry Automático** | Usa a mesma fila (`task.queue`) e o mesmo exchange/routing key para reprocessamento |
| **Escalabilidade** | Múltiplos workers podem consumir mensagens da mesma fila em paralelo |

---

## 🛠️ Stack Tecnológico

### Linguagem & Framework

| Tecnologia | Versão | Propósito |
|-----------|--------|----------|
| **Java** | 17 | Linguagem de programação |
| **Spring Boot** | 3.3.5 | Framework principal |
| **Maven** | Latest | Gerenciador de dependências |

### Componentes Spring Boot

| Componente | Propósito |
|-----------|----------|
| **Spring Web** | Desenvolvimento de APIs REST |
| **Spring Validation** | Validação de payloads |
| **Spring Security + JWT** | Autenticação e autorização |
| **Spring Data JPA** | Persistência de dados |
| **Spring AMQP** | Integração com RabbitMQ |

### Infraestrutura & Banco de Dados

| Tecnologia | Versão | Propósito |
|-----------|--------|----------|
| **PostgreSQL** | 16 | Banco de dados relacional |
| **RabbitMQ** | 3.13 | Message broker |
| **Flyway** | Latest | Migrações de banco de dados |
| **Docker** | Latest | Containerização |
| **Docker Compose** | Latest | Orquestração de containers |

### Documentação & Ferramentas

| Tecnologia | Propósito |
|-----------|----------|
| **Springdoc OpenAPI** | Geração automática de documentação |
| **Swagger UI** | Interface de testes de API |
| **Lombok** | Redução de boilerplate Java |

---

## 📁 Estrutura do Repositório

```
.
├── api-service/
│   ├── src/main/java/
│   │   ├── controller/          # Endpoints REST
│   │   ├── service/             # Lógica de negócio
│   │   ├── entity/              # Entidades JPA
│   │   ├── config/              # Configurações Spring
│   │   └── messaging/           # Publicação de mensagens
│   ├── src/main/resources/
│   │   └── application.yml      # Configurações da aplicação
│   ├── src/db/migration/        # Scripts Flyway
│   └── Dockerfile
│
├── worker-service/
│   ├── src/main/java/
│   │   ├── consumer/            # Consumidor RabbitMQ
│   │   ├── worker/              # Lógica de processamento
│   │   ├── execution/           # Executor de tarefas
│   │   ├── entity/              # Entidades JPA
│   │   └── config/              # Configurações Spring
│   ├── src/main/resources/
│   │   └── application.yml      # Configurações da aplicação
│   └── Dockerfile
│
├── docs/
│   ├── API.md                   # Documentação de endpoints
│   ├── ARQUITETURA.md           # Detalhes arquiteturais
│   └── OPERACAO.md              # Guia de operação
│
├── docker-compose.yml           # Orquestração de containers
├── .env.example                 # Template de variáveis de ambiente
└── README.md                    # Este arquivo
```

---

## 🚀 Como Executar

### Opção 1: Docker Compose (Recomendado)

A forma mais rápida e recomendada de executar o projeto completo.

#### Passo 1: Configurar Variáveis de Ambiente

```bash
# Linux/macOS
cp .env.example .env

# Windows PowerShell
Copy-Item .env.example .env
```

Depois ajuste os valores sensíveis no `.env`:

```bash
# Credenciais PostgreSQL
DB_USERNAME=postgres
DB_PASSWORD=sua-senha-segura

# Credenciais RabbitMQ
RABBITMQ_USER=guest
RABBITMQ_PASSWORD=guest

# Segredo JWT
JWT_SECRET=sua-chave-secreta-muito-segura-aqui

# Rate Limit
RATE_LIMIT_REQUESTS=100
RATE_LIMIT_WINDOW_MINUTES=1
```

#### Passo 2: Subir o Ambiente

```bash
# Subir em primeiro plano (para ver logs)
docker compose up --build

# Subir em background
docker compose up --build -d
```

#### Passo 3: Verificar Serviços

| Serviço | URL | Credenciais |
|---------|-----|-------------|
| **API** | `http://localhost:8080` | — |
| **Swagger UI** | `http://localhost:8080/swagger-ui.html` | — |
| **OpenAPI JSON** | `http://localhost:8080/v3/api-docs` | — |
| **RabbitMQ Management** | `http://localhost:15672` | Ver `.env` |
| **PostgreSQL** | `localhost:5432` | Ver `.env` |

#### Passo 4: Parar o Ambiente

```bash
docker compose down
```

---

### Opção 2: Execução Local (Sem Containers para API/Worker)

Útil para desenvolvimento com hot-reload.

#### Passo 1: Subir Apenas Dependências

```bash
# Suba apenas PostgreSQL e RabbitMQ
docker compose up postgres rabbitmq -d
```

#### Passo 2: Executar Serviços com Maven

```bash
# Terminal 1 — API Service
cd api-service
mvn spring-boot:run

# Terminal 2 — Worker Service
cd worker-service
mvn spring-boot:run
```

#### Passo 3: Verificar Logs

Ambos os serviços estarão rodando localmente e você verá os logs em tempo real nos terminais.

---

## 🔐 Segurança

### Implementações Aplicadas

| Implementação | Descrição |
|--------------|-----------|
| **Autenticação JWT** | Endpoint `POST /api/v1/auth/token` para geração de tokens |
| **Endpoints Protegidos** | Endpoints de tarefas requerem token Bearer válido |
| **Rate Limiting** | Limite configurável para `POST /api/v1/tasks` |
| **Perfil Prod** | `ddl-auto=validate` e Flyway habilitado em produção |
| **Segredos Externalizados** | Sem hardcode, apenas variáveis de ambiente |

### Fluxo Básico de Autenticação

#### 1️⃣ Gerar Token JWT

```bash
curl -X POST "http://localhost:8080/api/v1/auth/token" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "api-user",
    "password": "sua-senha"
  }'
```

**Resposta:**

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 3600
}
```

#### 2️⃣ Chamar Endpoint Protegido

```bash
curl -X POST "http://localhost:8080/api/v1/tasks" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "type": "REPORT",
    "payload": "dados da tarefa"
  }'
```

### Endpoints Públicos vs Protegidos

| Endpoint | Autenticação | Descrição |
|----------|-------------|-----------|
| `POST /api/v1/auth/token` | ❌ Público | Geração de token JWT |
| `GET /swagger-ui.html` | ❌ Público | Interface Swagger |
| `GET /v3/api-docs` | ❌ Público | Documentação OpenAPI |
| `POST /api/v1/tasks` | ✅ JWT Bearer | Criar nova tarefa |
| `GET /api/v1/tasks/{id}` | ✅ JWT Bearer | Consultar tarefa |
| `GET /api/v1/tasks` | ✅ JWT Bearer | Listar tarefas |

---

## 📊 Fluxo de Processamento

### Ciclo Completo de uma Tarefa

```
1. Cliente autentica em POST /api/v1/auth/token
   ↓
2. Cliente envia POST /api/v1/tasks com JWT Bearer
   ↓
3. API valida payload
   ↓
4. API salva tarefa com status PENDING e retryCount=0
   ↓
5. API publica mensagem TaskMessage no RabbitMQ
   ↓
6. Worker consome mensagem
   ↓
7. Worker marca tarefa como PROCESSING
   ↓
8. Worker tenta executar a tarefa:
   ├─ ✅ Sucesso: grava COMPLETED e o resultado
   ├─ ❌ Falha: incrementa retryCount
   │  ├─ Se retryCount < max-retries (default 3):
   │  │  └─ Volta para PENDING e reenfileira
   │  └─ Se retryCount >= max-retries:
   │     └─ Marca FAILED
```

### Estados e Transições

| Estado | Próximo Estado | Condição |
|--------|---|----------|
| **PENDING** | PROCESSING | Worker inicia processamento |
| **PROCESSING** | COMPLETED | Processamento bem-sucedido |
| **PROCESSING** | PENDING | Falha e retryCount < max-retries |
| **PROCESSING** | FAILED | Falha e retryCount >= max-retries |
| **COMPLETED** | — | Estado final |
| **FAILED** | — | Estado final |

### Teste de Falha Controlada

Para testar o mecanismo de retry, envie um payload contendo `FORCE_ERROR`:

```bash
curl -X POST "http://localhost:8080/api/v1/tasks" \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "REPORT",
    "payload": "FORCE_ERROR"
  }'
```

O Worker gerará um erro intencional e ativará a política de retry automático.

---

## 📚 Documentação Detalhada

Para informações mais profundas, consulte os documentos específicos:

| Documento | Conteúdo |
|-----------|----------|
| **[API.md](docs/API.md)** | Documentação completa de endpoints, payloads e respostas |
| **[ARQUITETURA.md](docs/ARQUITETURA.md)** | Detalhes arquiteturais, ciclo de vida e padrões de design |
| **[OPERACAO.md](docs/OPERACAO.md)** | Guia de operação, troubleshooting e monitoramento |

---

## 🎓 Conceitos-Chave

### Idempotência

O sistema garante que processar a mesma mensagem múltiplas vezes não causa efeitos colaterais indesejados. Se uma tarefa já foi concluída, o worker a ignora.

### Escalabilidade Horizontal

Múltiplos workers podem ser executados em paralelo, cada um consumindo mensagens da mesma fila. O RabbitMQ distribui as mensagens entre os workers automaticamente.

### Resiliência

Com retry automático configurável, o sistema tenta reprocessar tarefas que falharam temporariamente, aumentando a taxa de sucesso geral.

### Separação de Responsabilidades

- **API Service**: Responsável por receber requisições e persistir dados
- **Worker Service**: Responsável por processar tarefas de forma assincronica
- **RabbitMQ**: Responsável por desacoplar os serviços e garantir entrega de mensagens

---

## 📞 Suporte

Para dúvidas ou problemas, consulte a documentação detalhada ou abra uma issue no repositório.

**Versão:** 1.0.0  
**Última atualização:** 2026-04-23
