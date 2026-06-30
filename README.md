# 🔧 Oficina Mecânica — Sistema de Gestão de Ordens de Serviço (MVP)

> Back-end do sistema integrado de atendimento e execução de serviços para uma oficina mecânica de médio porte. Permite gerenciar clientes, veículos, ordens de serviço, orçamentos, peças e funcionários, com acompanhamento em tempo real e autorização de reparos via aplicativo.

Desenvolvido como parte do **Tech Challenge — FIAP 16SOAT**.

---

## 📑 Índice

- [Stack Tecnológica](#-stack-tecnológica)
- [Arquitetura e Design](#-arquitetura-e-design)
- [Estrutura do Projeto](#-estrutura-do-projeto)
- [Modelo de Domínio](#-modelo-de-domínio)
- [Segurança e Controle de Acesso](#-segurança-e-controle-de-acesso)
- [Endpoints da API](#-endpoints-da-api)
- [Infraestrutura (Docker)](#-infraestrutura-docker)
- [Como Executar](#-como-executar)
- [Testes e Cobertura](#-testes-e-cobertura)
- [Análise de Qualidade (SonarQube)](#-análise-de-qualidade-sonarqube)
- [Variáveis de Ambiente](#-variáveis-de-ambiente)
- [Diagramas](#-diagramas)

---

## 🛠 Stack Tecnológica

| Categoria                    | Tecnologia                                              |
|------------------------------|---------------------------------------------------------|
| Linguagem                    | Java 21 (LTS)                                           |
| Framework                    | Spring Boot 4.0.6                                       |
| Gerenciador de Dependências  | Maven 3.9+                                              |
| Banco de Dados               | PostgreSQL 16                                           |
| Migração de Schema           | Flyway                                                  |
| Autenticação / Autorização   | Keycloak 24 + Spring Security OAuth2 Resource Server    |
| Mapeamento de Objetos        | MapStruct 1.5.5.Final + Lombok 1.18.38                  |
| Documentação da API          | Springdoc OpenAPI UI (Swagger) 3.0.3                    |
| Testes                       | JUnit 5 + Testcontainers (PostgreSQL real)              |
| Cobertura de Código          | JaCoCo 0.8.12 (mínimo 80% de linhas)                   |
| Análise de Qualidade         | SonarQube (Community Edition)                           |
| Containerização              | Docker + Docker Compose                                 |

---

## 📐 Arquitetura e Design

A aplicação segue os princípios de **Arquitetura em Camadas** combinada com **Domain-Driven Design (DDD)**:

```
┌─────────────────────────────────────────────┐
│              Controller (REST)               │  ← Entrada HTTP / Validação
├─────────────────────────────────────────────┤
│                  Service                     │  ← Casos de Uso / Orquestração
├─────────────────────────────────────────────┤
│              Domain (Entidades)              │  ← Regras de Negócio Encapsuladas
├─────────────────────────────────────────────┤
│               Repository (JPA)              │  ← Persistência
└─────────────────────────────────────────────┘
```

**Princípios aplicados:**
- **Modelos Ricos:** lógica de negócio encapsulada nas entidades de domínio, não nos services.
- **Value Objects:** Java Records para imutabilidade — `CPF`, `CNPJ`, `Placa`, `Endereco`.
- **Herança de Domínio:** `Cliente` (abstrato) → `ClientePF` / `ClientePJ`; `Funcionario` (abstrato) → `Atendente` / `Mecanico`.
- **Separação de DTOs:** objetos de entrada/saída isolados da camada de domínio via MapStruct.
- **Profiles Spring:** `dev` (local com Docker Compose) e `prod` (variáveis de ambiente externas).

---

## 📂 Estrutura do Projeto

```
src/
├── main/
│   ├── java/com/autopecas/autopecas/
│   │   ├── AutopecasApplication.java          # Entry point Spring Boot
│   │   ├── config/                            # Beans de configuração (OpenAPI, etc.)
│   │   ├── controller/                        # Controllers REST
│   │   │   ├── ClienteController.java
│   │   │   ├── VeiculoController.java
│   │   │   ├── FuncionarioController.java
│   │   │   ├── PecaController.java
│   │   │   ├── ServicoController.java
│   │   │   ├── OrdemServicoController.java
│   │   │   ├── OrcamentoController.java
│   │   │   └── DashboardController.java
│   │   ├── domain/
│   │   │   ├── entity/                        # Entidades JPA do domínio (17 classes)
│   │   │   ├── enums/                         # Enumerações do negócio (7 enums)
│   │   │   └── valueobject/                   # Value Objects imutáveis (Records)
│   │   ├── dto/                               # DTOs de entrada e saída por módulo
│   │   ├── exception/                         # Exceções de negócio customizadas
│   │   ├── mapper/                            # Interfaces MapStruct
│   │   ├── repository/                        # Interfaces Spring Data JPA
│   │   ├── security/                          # Configuração de segurança e JWT Converter
│   │   └── service/                           # Casos de uso e regras de aplicação
│   └── resources/
│       ├── application.yml                    # Configuração base (profile ativo)
│       ├── application-dev.yml                # Configuração perfil DEV
│       ├── application-prod.yml               # Configuração perfil PROD
│       └── db/migration/
│           ├── V1__schema_inicial.sql          # Criação das tabelas
│           └── V2__add_sequences.sql           # Sequências de numeração
├── test/
│   ├── java/com/autopecas/autopecas/
│   │   ├── controller/                        # Testes de Controller (MockMvc)
│   │   ├── domain/entity/                     # Testes unitários de entidades
│   │   └── AutopecasApplicationTests.java     # Teste de contexto
│   └── resources/
│       ├── application.yml                    # Configuração de testes
│       └── init-test.sql                      # Script de dados iniciais para testes
├── keycloak/
│   └── realm-export.json                      # Configuração do realm (import automático)
├── Dockerfile                                 # Build multi-stage (Maven → JRE)
└── docker-compose.yml                         # Orquestração de todos os serviços
```

---

## 🗂 Modelo de Domínio

### Entidades Principais

| Entidade               | Descrição                                                    |
|------------------------|--------------------------------------------------------------|
| `Cliente`              | Abstrato — base para PF e PJ                                 |
| `ClientePF`            | Cliente Pessoa Física (CPF)                                  |
| `ClientePJ`            | Cliente Pessoa Jurídica (CNPJ)                               |
| `Veiculo`              | Veículo cadastrado (placa, modelo, ano)                      |
| `Funcionario`          | Abstrato — base para Atendente e Mecânico                    |
| `Atendente`            | Funcionário responsável pelo atendimento                     |
| `Mecanico`             | Funcionário executor dos serviços                            |
| `OrdemServico`         | OS com ciclo de vida completo e histórico de status          |
| `HistoricoStatusOS`    | Registro de cada transição de status da OS                   |
| `Orcamento`            | Orçamento vinculado a uma OS                                 |
| `ItemOrcamentoPeca`    | Peça incluída em um orçamento                                |
| `ItemOrcamentoServico` | Serviço incluído em um orçamento                             |
| `ItemPecaOS`           | Peça efetivamente utilizada na OS                            |
| `ItemServicoOS`        | Serviço efetivamente executado na OS                         |
| `Peca`                 | Peça do estoque (com controle de quantidade mínima)          |
| `Servico`              | Serviço disponível no catálogo                               |
| `MovimentacaoEstoque`  | Registro de entrada/saída de peças                           |

### Value Objects

| Value Object | Descrição                                     |
|--------------|-----------------------------------------------|
| `CPF`        | Validação e formatação de CPF                 |
| `CNPJ`       | Validação e formatação de CNPJ                |
| `Placa`      | Validação de placa (padrão Mercosul e antigo) |
| `Endereco`   | Endereço completo                             |

### Enumerações

| Enum                      | Valores                                                    |
|---------------------------|------------------------------------------------------------|
| `StatusOS`                | `ABERTA`, `EM_DIAGNOSTICO`, `AGUARDANDO_APROVACAO`, `APROVADA`, `EM_EXECUCAO`, `CONCLUIDA`, `CANCELADA` |
| `StatusOrcamento`         | `RASCUNHO`, `ENVIADO`, `APROVADO`, `REJEITADO`             |
| `StatusItemOS`            | `PENDENTE`, `EM_EXECUCAO`, `CONCLUIDO`                     |
| `TipoCliente`             | `PF`, `PJ`                                                 |
| `TipoFuncionario`         | `ATENDENTE`, `MECANICO`                                    |
| `Genero`                  | `MASCULINO`, `FEMININO`, `OUTRO`                           |
| `TipoMovimentacaoEstoque` | `ENTRADA`, `SAIDA`                                         |

---

## 🔐 Segurança e Controle de Acesso

A autenticação é feita via **JWT emitido pelo Keycloak**. A aplicação atua como **OAuth2 Resource Server**, validando os tokens através do endpoint JWKS do Keycloak.

### Perfis de Acesso (Roles)

| Role        | Permissões                                                                        |
|-------------|-----------------------------------------------------------------------------------|
| `ADMIN`     | Acesso total a todos os endpoints                                                 |
| `ATENDENTE` | Gerencia clientes, veículos, OS, orçamentos e movimentações de estoque            |
| `MECANICO`  | Consulta serviços e peças disponíveis                                             |
| `CLIENTE`   | Consulta o próprio cadastro, veículos próprios, OS e orçamentos das suas OS       |

### Keycloak (ambiente Docker)
- **URL:** `http://localhost:9080`
- **Usuário admin:** `admin` / `admin`
- **Realm:** `app-realm` (importado automaticamente via `keycloak/realm-export.json`)
- **Issuer URI:** `http://keycloak:8080/realms/app-realm`

---

## 🌐 Endpoints da API

A documentação interativa completa está disponível via Swagger após subir a aplicação:

> **Swagger UI:** `http://localhost:8080/swagger-ui.html`  
> **OpenAPI JSON:** `http://localhost:8080/v3/api-docs`

### Clientes — `/api/clientes`

| Método   | Endpoint                        | Roles                            | Descrição                          |
|----------|---------------------------------|----------------------------------|------------------------------------|
| `GET`    | `/api/clientes`                 | `ADMIN`, `ATENDENTE`             | Lista todos os clientes            |
| `GET`    | `/api/clientes/{id}`            | `ADMIN`, `ATENDENTE`, `CLIENTE`  | Busca cliente por ID               |
| `GET`    | `/api/clientes/buscarDOC?documento=` | `ADMIN`, `ATENDENTE`        | Busca por CPF/CNPJ                 |
| `POST`   | `/api/clientes/pf`              | `ADMIN`, `ATENDENTE`             | Cadastra cliente Pessoa Física     |
| `POST`   | `/api/clientes/pj`              | `ADMIN`, `ATENDENTE`             | Cadastra cliente Pessoa Jurídica   |
| `PUT`    | `/api/clientes/{id}`            | `ADMIN`, `ATENDENTE`             | Atualiza dados do cliente          |
| `DELETE` | `/api/clientes/{id}`            | `ADMIN`                          | Desativa cliente                   |

### Veículos — `/api/veiculos`

| Método   | Endpoint                             | Roles                            | Descrição                          |
|----------|--------------------------------------|----------------------------------|------------------------------------|
| `GET`    | `/api/veiculos`                      | `ADMIN`, `ATENDENTE`             | Lista todos os veículos            |
| `GET`    | `/api/veiculos/{id}`                 | `ADMIN`, `ATENDENTE`, `CLIENTE`  | Busca veículo por ID               |
| `GET`    | `/api/veiculos/placa/{placa}`        | `ADMIN`, `ATENDENTE`             | Busca veículo por placa            |
| `GET`    | `/api/veiculos/cliente/{clienteId}`  | `ADMIN`, `ATENDENTE`, `CLIENTE`  | Lista veículos de um cliente       |
| `POST`   | `/api/veiculos`                      | `ADMIN`, `ATENDENTE`             | Cadastra veículo                   |
| `PUT`    | `/api/veiculos/{id}`                 | `ADMIN`, `ATENDENTE`             | Atualiza veículo                   |
| `DELETE` | `/api/veiculos/{id}`                 | `ADMIN`                          | Remove veículo                     |

### Funcionários — `/api/funcionarios`

| Método   | Endpoint                       | Roles                | Descrição                   |
|----------|--------------------------------|----------------------|-----------------------------|
| `GET`    | `/api/funcionarios`            | `ADMIN`, `ATENDENTE` | Lista todos os funcionários |
| `GET`    | `/api/funcionarios/{id}`       | `ADMIN`, `ATENDENTE` | Busca funcionário por ID    |
| `POST`   | `/api/funcionarios/mecanico`   | `ADMIN`              | Cadastra mecânico           |
| `POST`   | `/api/funcionarios/atendente`  | `ADMIN`              | Cadastra atendente          |
| `DELETE` | `/api/funcionarios/{id}`       | `ADMIN`              | Desativa funcionário        |

### Peças e Estoque — `/api/pecas`

| Método   | Endpoint                        | Roles                             | Descrição                                    |
|----------|---------------------------------|-----------------------------------|----------------------------------------------|
| `GET`    | `/api/pecas`                    | `ADMIN`, `ATENDENTE`, `MECANICO`  | Lista peças (filtro `?estoqueBaixo=true`)    |
| `GET`    | `/api/pecas/{id}`               | `ADMIN`, `ATENDENTE`, `MECANICO`  | Busca peça por ID                            |
| `GET`    | `/api/pecas/buscar?codigo=`     | `ADMIN`, `ATENDENTE`, `MECANICO`  | Busca peça por código                        |
| `POST`   | `/api/pecas`                    | `ADMIN`                           | Cadastra peça                                |
| `PUT`    | `/api/pecas/{id}`               | `ADMIN`                           | Atualiza peça                                |
| `DELETE` | `/api/pecas/{id}`               | `ADMIN`                           | Desativa peça                                |
| `POST`   | `/api/pecas/{id}/movimentacoes` | `ADMIN`, `ATENDENTE`              | Registra entrada/saída de estoque            |

### Serviços — `/api/servicos`

| Método   | Endpoint             | Roles                             | Descrição               |
|----------|----------------------|-----------------------------------|-------------------------|
| `GET`    | `/api/servicos`      | `ADMIN`, `ATENDENTE`, `MECANICO`  | Lista todos os serviços |
| `GET`    | `/api/servicos/{id}` | `ADMIN`, `ATENDENTE`, `MECANICO`  | Busca serviço por ID    |
| `POST`   | `/api/servicos`      | `ADMIN`                           | Cadastra serviço        |
| `PUT`    | `/api/servicos/{id}` | `ADMIN`                           | Atualiza serviço        |
| `DELETE` | `/api/servicos/{id}` | `ADMIN`                           | Desativa serviço        |

### Ordens de Serviço — `/api/ordens-servico`

| Método  | Endpoint                                | Roles        | Descrição                                                    |
|---------|-----------------------------------------|--------------|--------------------------------------------------------------|
| `GET`   | `/api/ordens-servico`                   | Autenticado  | Lista OS (filtros: `status`, `clienteId`, `mecanicoId`, paginação) |
| `GET`   | `/api/ordens-servico/{numero}`          | Autenticado  | Busca OS pelo número                                         |
| `POST`  | `/api/ordens-servico`                   | Autenticado  | Cria nova OS                                                 |
| `PATCH` | `/api/ordens-servico/{id}/status`       | Autenticado  | Avança o status da OS                                        |
| `PATCH` | `/api/ordens-servico/{id}/diagnostico`  | Autenticado  | Registra diagnóstico na OS                                   |
| `PATCH` | `/api/ordens-servico/{id}/mecanico`     | Autenticado  | Atribui mecânico à OS                                        |

### Orçamentos — `/api/ordens-servico/{osId}/orcamentos`

| Método  | Endpoint                                               | Roles                            | Descrição                    |
|---------|--------------------------------------------------------|----------------------------------|------------------------------|
| `POST`  | `/api/ordens-servico/{osId}/orcamentos`                | `ADMIN`, `ATENDENTE`             | Cria orçamento para a OS     |
| `GET`   | `/api/ordens-servico/{osId}/orcamentos`                | `ADMIN`, `ATENDENTE`, `CLIENTE`  | Lista orçamentos da OS       |
| `PATCH` | `/api/ordens-servico/{osId}/orcamentos/{id}/enviar`    | `ADMIN`, `ATENDENTE`             | Envia orçamento ao cliente   |
| `PATCH` | `/api/ordens-servico/{osId}/orcamentos/{id}/aprovar`   | `ADMIN`, `ATENDENTE`, `CLIENTE`  | Aprova orçamento             |
| `PATCH` | `/api/ordens-servico/{osId}/orcamentos/{id}/rejeitar`  | `ADMIN`, `ATENDENTE`, `CLIENTE`  | Rejeita orçamento            |

### Dashboard — `/api/dashboard`

| Método | Endpoint                               | Roles   | Descrição                            |
|--------|----------------------------------------|---------|--------------------------------------|
| `GET`  | `/api/dashboard`                       | `ADMIN` | Visão geral (KPIs da oficina)        |
| `GET`  | `/api/dashboard/tempo-medio-execucao`  | `ADMIN` | Tempo médio de execução por serviço  |

---

## 🐳 Infraestrutura (Docker)

O `docker-compose.yml` orquestra 5 serviços:

| Serviço          | Imagem                         | Porta  | Descrição                                |
|------------------|--------------------------------|--------|------------------------------------------|
| `postgres`       | `postgres:16-alpine`           | `5432` | Banco da aplicação e do Keycloak         |
| `postgres_sonar` | `postgres:16-alpine`           | —      | Banco exclusivo do SonarQube             |
| `sonarqube`      | `sonarqube:community`          | `9000` | Análise estática de código               |
| `keycloak`       | `keycloak/keycloak:24.0.0`     | `9080` | Identity Provider (IdP) com realm pronto |
| `app`            | Build local (`Dockerfile`)     | `8080` | Aplicação Spring Boot                    |

### Dockerfile (Multi-stage Build)

```
Stage 1 (build): maven:3.9-eclipse-temurin-21
  └── mvn clean package -DskipTests

Stage 2 (runtime): eclipse-temurin:21-jre
  └── Usuário não-root (appuser)
  └── JVM otimizada para container (-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0)
```

---

## 🚀 Como Executar

### Pré-requisitos

- JDK 21+
- Maven 3.9+
- Docker Engine + Docker Compose v2

### 1. Clonar o repositório

```bash
git clone <repository-url>
cd 16SOAT-TechChallenge1
```

### 2. Subir toda a infraestrutura com a aplicação

```bash
docker-compose up --build -d
```

Aguarde os health checks passarem. A ordem de inicialização é gerenciada automaticamente:  
`postgres` → `keycloak` → `app`

### 3. Verificar os serviços

| Serviço    | URL                                   |
|------------|---------------------------------------|
| API        | http://localhost:8080                 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Keycloak   | http://localhost:9080                 |
| SonarQube  | http://localhost:9000                 |

### 4. Executar apenas a infraestrutura (para desenvolvimento local)

```bash
# Sobe apenas postgres, keycloak e sonarqube (sem o app)
docker-compose up postgres keycloak sonarqube -d

# Rode a aplicação com o profile dev
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

---

## 🧪 Testes e Cobertura

### Executar os testes

```bash
./mvnw test
```

### Executar com relatório de cobertura (JaCoCo)

```bash
./mvnw clean verify
```

O relatório HTML é gerado em `target/site/jacoco/index.html`.

### Estratégia de Testes

| Tipo                    | Descrição                                                                      |
|-------------------------|--------------------------------------------------------------------------------|
| **Testes Unitários**    | Entidades de domínio isoladas — sem Spring Context                             |
| **Testes de Controller**| MockMvc com `@WebMvcTest` e `@WithMockUser`                                    |
| **Testes de Integração**| Testcontainers sobe um PostgreSQL real para validar queries e migrações Flyway |
| **Teste de Contexto**   | `AutopecasApplicationTests` valida que o Spring Context sobe com profile `dev` |

### Gate de Cobertura (JaCoCo)

A build falha automaticamente se a cobertura de linhas ficar abaixo de **80%**.

Classes excluídas da contagem (sem lógica de negócio):
- `AutopecasApplication` (entry point)
- `dto/**` (Records)
- `mapper/**Mapper.class` / `**MapperImpl.class` (gerados pelo MapStruct)
- `domain/enums/**`
- `config/**`
- Exceções simples (`BusinessException`, `ResourceNotFoundException`, `EstoqueInsuficienteException`)

---

## 📊 Análise de Qualidade (SonarQube)

### Configuração inicial do projeto no SonarQube

1. Acesse `http://localhost:9000` e faça login com `admin` / `admin`
2. Redefina a senha quando solicitado
3. Clique em **Create Project** → **Manually**
4. Preencha:
   - **Project Key:** `16SOAT-TechChallenge`
   - **Display Name:** `16SOAT-TechChallenge`
5. Escolha **Locally** e clique em **Generate** para criar o token
6. Copie o token gerado (exibido apenas uma vez)

### Executar análise

```bash
./mvnw clean verify sonar:sonar "-Dsonar.token=<seu_token>"
```

Aguarde `BUILD SUCCESS` e acesse o dashboard em `http://localhost:9000`.

> **Observação:** O `verify` executa os testes de integração com Testcontainers, que requerem o Docker em execução.

---

## ⚙️ Variáveis de Ambiente

### Profile `dev` (padrão local)

| Variável                 | Valor padrão                              |
|--------------------------|-------------------------------------------|
| `DB_URL`                 | `jdbc:postgresql://localhost:5432/app_db` |
| `DB_USERNAME`            | `postgres`                                |
| `DB_PASSWORD`            | `postgres`                                |
| `SPRING_PROFILES_ACTIVE` | `dev`                                     |

### Profile `prod`

| Variável                   | Descrição                                               |
|----------------------------|---------------------------------------------------------|
| `DB_URL`                   | URL de conexão com o banco PostgreSQL                   |
| `DB_USERNAME`              | Usuário do banco                                        |
| `DB_PASSWORD`              | Senha do banco                                          |
| `KEYCLOAK_ISSUER_URI`      | URI do realm Keycloak (`http://<host>/realms/<realm>`)  |
| `KEYCLOAK_CLIENT_ID`       | Client ID configurado no Keycloak (padrão: `autopecas-api`) |
| `KEYCLOAK_AUTH_SERVER_URL` | URL base do Keycloak                                    |
| `KEYCLOAK_REALM`           | Nome do realm                                           |
| `SPRING_PROFILES_ACTIVE`   | `prod`                                                  |

### Docker Compose (`app` service)

```yaml
environment:
  SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/app_db
  SPRING_DATASOURCE_USERNAME: postgres
  SPRING_DATASOURCE_PASSWORD: postgres
  SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI: http://keycloak:8080/realms/app-realm
```

---

## 📎 Diagramas

Diagramas de **Domain Storytelling** e **Event Storming**:

🔗 [Abrir no Miro](https://miro.com/app/board/uXjVHTmOiSE=/)

---

## 👥 Equipe

Desenvolvido como parte do **Tech Challenge** da **FIAP — Turma 16SOAT**.
