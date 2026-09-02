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
- [Infraestrutura e Execução](#-infraestrutura-e-execução)
  - [Pré-requisitos](#pré-requisitos)
  - [Containerização (Docker)](#containerização-docker)
  - [Provisionamento (Terraform)](#provisionamento-terraform)
  - [Orquestração (Kubernetes)](#orquestração-kubernetes)
  - [Passo a passo completo](#passo-a-passo-completo)
  - [Autoscaling](#autoscaling-como-verificar)
  - [Segredos em produção](#segredos-em-produção)
  - [Operação e diagnóstico](#operação-e-diagnóstico)
- [CI/CD Local (Self-hosted Runner)](#-cicd-local-self-hosted-runner)
  - [Como funciona o pipeline](#como-funciona-o-pipeline)
  - [Pré-requisitos da máquina](#pré-requisitos-da-máquina)
  - [Instalar o GitHub Actions Runner](#instalar-o-github-actions-runner)
  - [Registrar o runner no repositório](#registrar-o-runner-no-repositório)
  - [Iniciar o runner](#iniciar-o-runner)
  - [Secret necessário](#secret-necessário)
  - [Executar o pipeline manualmente](#executar-o-pipeline-manualmente)
  - [Verificar o runner no GitHub](#verificar-o-runner-no-github)
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
| Fronteiras de Arquitetura    | ArchUnit 1.3.0 (10 regras, quebram a build)             |
| Cobertura de Código          | JaCoCo 0.8.12 (mínimo 80% de linhas)                   |
| Análise de Qualidade         | SonarQube (Community Edition)                           |
| Containerização              | Docker (build em 3 estágios) + Docker Compose           |
| Orquestração                 | Kubernetes — Deployments, Services, ConfigMap/Secrets, HPA |
| Infraestrutura como Código   | Terraform (kind + PostgreSQL + metrics-server)          |

---

## 📐 Arquitetura e Design

A aplicação segue **Arquitetura Hexagonal (Ports & Adapters)** com **Domain-Driven Design**. A
regra que organiza tudo é a **direção da dependência**: ela aponta sempre para dentro.

```
                     ┌──────────────────────────────────────┐
                     │            ADAPTERS (fora)           │
   HTTP / Keycloak ──┤  adapter/in/web                      │
                     │      ↓ chama inbound port            │
                     │  ┌────────────────────────────────┐  │
                     │  │      APPLICATION (casos de uso) │  │
                     │  │  port/in  ·  usecase  ·  port/out│  │
                     │  │  ┌──────────────────────────┐   │  │
                     │  │  │    DOMAIN (Java puro)    │   │  │
                     │  │  │ agregados · VOs · regras │   │  │
                     │  │  └──────────────────────────┘   │  │
                     │  └────────────────────────────────┘  │
                     │      ↑ implementa outbound port      │
   PostgreSQL ───────┤  adapter/out/persistence · clock ·   │
                     │  numbering · tx                      │
                     └──────────────────────────────────────┘
```

```bash
javac -d /tmp/dominio $(find src/main/java/com/autopecas/autopecas/domain -name "*.java")
```

**Princípios aplicados:**
- **Ports & Adapters:** 9 inbound ports e 16 outbound ports. A aplicação
  declara o que precisa; quem implementa é sempre um adapter.
- **Modelos ricos e encapsulados:** construtores privados, factory methods (`abrir`, `criar`,
  `reconstituir`), coleções imutáveis e **nenhum setter público** — a regra de negócio não pode
  ser burlada de fora.
- **Value Objects imutáveis:** `CPF`, `CNPJ`, `Placa`, `Endereco` como Java records, validando no
  construtor.
- **Referências entre agregados por id**, não por objeto — o que elimina o acoplamento a
  lazy-loading do Hibernate.
- **Persistência separada do domínio:** as entidades JPA (`*JpaEntity`) vivem no adapter, e
  mappers escritos à mão convertem nos dois sentidos.
- **Tempo e numeração como ports:** `Relogio` e `GeradorNumeroOS`/`GeradorMatricula`. O domínio
  nunca chama `LocalDateTime.now()` — recebe o instante por parâmetro, o que torna as regras
  determinísticas.
- **Transação como port:** `Transacao` (implementada com `TransactionTemplate`). Nenhum caso de
  uso conhece `@Transactional`.
- **Paginação própria:** `PaginaRequisicao`/`Pagina` atravessam as ports; `Pageable`/`Page` do
  Spring ficam confinados ao adapter web, que remonta o envelope JSON original.
- **CQRS-lite nas leituras:** listagens e dashboard usam *query ports* com projeção SQL, evitando
  o N+1 que existiria ao carregar agregados só para ler nome do cliente e placa.
- **Fronteiras verificadas por ArchUnit:** 10 regras que quebram a build se alguém furar o
  hexágono (ver `arquitetura/ArquiteturaHexagonalTest`).

---

## 📂 Estrutura do Projeto

```
src/main/java/com/autopecas/autopecas/
├── AutopecasApplication.java
│
├── domain/                          # ZERO dependências — nem framework, nem Lombok
│   ├── model/
│   │   ├── cliente/                 # Cliente (abstrato), ClientePF, ClientePJ
│   │   ├── funcionario/             # Funcionario (abstrato), Atendente, Mecanico
│   │   ├── veiculo/                 # Veiculo
│   │   ├── os/                      # OrdemServico (raiz), ItemServicoOS, ItemPecaOS,
│   │   │                            #   HistoricoStatusOS
│   │   ├── orcamento/               # Orcamento (raiz), ItemOrcamentoServico, ItemOrcamentoPeca
│   │   └── estoque/                 # Peca, Servico, MovimentacaoEstoque
│   ├── vo/                          # CPF, CNPJ, Placa, Endereco (records)
│   ├── enums/                       # 7 enums do negócio
│   ├── exception/                   # BusinessException, ResourceNotFound, EstoqueInsuficiente
│   └── service/                     # MovimentadorDeEstoque (domain service puro)
│
├── application/                     # Depende SÓ de domain + JDK
│   ├── port/in/                     # 9 inbound ports + commands aninhados
│   │   └── view/                    # Projeções de leitura (records)
│   ├── port/out/                    # Repositórios, query ports, Relogio, geradores, Transacao, Notificador
│   ├── pagination/                  # PaginaRequisicao, Pagina
│   └── usecase/                     # 9 casos de uso, sem anotação de framework
│
├── adapter/
│   ├── in/web/                      # Controllers REST, DTOs, mappers MapStruct, handler de erro
│   └── out/
│       ├── persistence/             # entity/ (JPA), repository/ (Spring Data), mapper/,
│       │                            #   adapter/ (implementa as ports), projection/
│       ├── clock/                   # RelogioSistema
│       ├── numbering/               # Geradores sobre sequences do Postgres
│       └── tx/                      # TransacaoSpring
│
├── config/                          # UseCaseConfig (wiring), SecurityConfig, OpenApiConfig
└── security/                        # KeycloakJwtAuthConverter, PropriedadeDoRecurso

src/test/java/com/autopecas/autopecas/
├── arquitetura/                     # ArquiteturaHexagonalTest — fronteiras como teste
├── domain/                          # Testes puros: sem Spring, sem mocks, sem banco
├── application/
│   ├── fake/                        # Fakes das ports (TransacaoDireta, RelogioFixo, geradores)
│   └── usecase/                     # Testes de caso de uso contra dublês das ports
└── integration/                     # Testcontainers + PostgreSQL real (schema via Flyway)
```

Infraestrutura (detalhada na seção [Infraestrutura e Execução](#-infraestrutura-e-execução)):

```
Dockerfile                           # build em 3 estágios, JAR em camadas
docker-compose.yml                   # stack local; SonarQube atrás do profile `qa`
docker/postgres/                     # script que cria o keycloak_db no 1º boot
keycloak/realm-export.json           # realm — fonte única, usada por Compose e K8s

infra/
├── k8s/                             # aplicado com `kubectl apply -f infra/k8s/`
│   ├── 00-namespace.yaml
│   ├── 10-configmap-app.yaml        # configuração não sensível
│   ├── 11-secret-app.yaml           # credenciais (valores de desenvolvimento)
│   ├── 20-keycloak.yaml             # Deployment + Service + NodePort
│   ├── 30-app-deployment.yaml       # Deployment + Service + NodePort + PDB
│   └── 40-app-hpa.yaml              # HPA: CPU 70% / memória 85%, 2–5 réplicas
│
└── terraform/                       # provisiona o que precede a aplicação
    ├── versions.tf                  # providers kind, kubernetes, helm
    ├── cluster.tf                   # cluster kind + namespace
    ├── database.tf                  # PostgreSQL (StatefulSet + PVC + Service)
    ├── keycloak-realm.tf            # ConfigMap do realm, lido do arquivo único
    ├── metrics-server.tf            # pré-requisito do HPA
    ├── variables.tf / outputs.tf
    └── terraform.tfvars.example
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
| `StatusOS`                | `RECEBIDA`, `EM_DIAGNOSTICO`, `AGUARDANDO_APROVACAO`, `EM_EXECUCAO`, `FINALIZADA`, `ENTREGUE` |
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

### Propriedade do recurso

A role `CLIENTE` diz apenas *"é um cliente"*, não *"é o dono deste registro"*. Por isso, nos
endpoints marcados com † nas tabelas abaixo, a autorização soma à role uma **checagem de
propriedade**: `@PreAuthorize` chama o bean `@propriedade` (`PropriedadeDoRecurso`), que delega
ao `ControleDeAcessoDoClienteUseCase` — a inbound port responsável por resolver o cadastro do
usuário autenticado e comparar com o dono do recurso.

O vínculo entre o usuário do Keycloak e o cadastro é o **e-mail**: o claim `email` do token é
casado com `clientes.email` (coluna `UNIQUE`), considerando apenas clientes ativos. Um cliente
cadastrado com e-mail diferente do que usa no Keycloak não consegue acessar os próprios dados —
é o custo de não haver um identificador federado explícito no schema.

Quem extrai o e-mail do token é o `KeycloakJwtAuthConverter`, que define o nome do principal
como `email` → `preferred_username` → `sub` (nessa ordem de preferência).

#### Como reproduzir o teste de propriedade

O realm importado traz apenas `test_user` (ADMIN). Para exercitar a role `CLIENTE` é preciso
criar um usuário cujo e-mail case com um cliente cadastrado:

```bash
ADMIN=$(curl -s -X POST "http://localhost:9080/realms/autopecas/protocol/openid-connect/token"   -H "Content-Type: application/x-www-form-urlencoded"   -d "grant_type=password" -d "client_id=autopecas-api"   -d "username=test_user" -d "password=1234" | jq -r .access_token)

# 1. Cadastrar o cliente pela API
curl -s -X POST http://localhost:8080/api/clientes/pf   -H "Authorization: Bearer $ADMIN" -H "Content-Type: application/json"   -d '{"nome":"Cliente A","email":"cliente.a@autopecas.com","telefone":"11999990001",
       "aceitaNotificacoes":true,"cpf":"52998224725","dataNascimento":"1990-01-01"}'

# 2. Criar o usuário no Keycloak com o MESMO e-mail
KCADM=$(curl -s -X POST "http://localhost:9080/realms/master/protocol/openid-connect/token"   -H "Content-Type: application/x-www-form-urlencoded"   -d "grant_type=password" -d "client_id=admin-cli"   -d "username=admin" -d "password=admin" | jq -r .access_token)

curl -s -X POST "http://localhost:9080/admin/realms/autopecas/users"   -H "Authorization: Bearer $KCADM" -H "Content-Type: application/json"   -d '{"username":"cliente.a","email":"cliente.a@autopecas.com","emailVerified":true,
       "firstName":"Cliente","lastName":"A","enabled":true,
       "credentials":[{"type":"password","value":"1234","temporary":false}]}'
```

Depois atribua ao usuário os roles **`CLIENTE`** e **`default-roles-autopecas`** (via console em
`Users → Role mapping`, ou pela API em `/users/{id}/role-mappings/realm`).

> Duas armadilhas ao criar o usuário pela API, ambas resultando em
> `invalid_grant: Account is not fully set up` na hora de pedir o token:
> **`firstName`/`lastName` são obrigatórios** (o realm tem a required action *Verify Profile*), e
> **`default-roles-autopecas` não é atribuído automaticamente**. Pelo console os dois já vêm
> resolvidos.

#### Resultado verificado

Com o `Cliente A` autenticado, contra um `Cliente B` cadastrado no mesmo banco:

| Requisição | Esperado | Obtido |
|---|---|---|
| `GET /api/clientes/{id-do-A}` | 200 | ✅ 200 |
| `GET /api/clientes/{id-do-B}` | 403 | ✅ 403 |
| `GET /api/clientes` (lista) | 403 | ✅ 403 |
| `GET /api/veiculos/cliente/{id-do-A}` | 200 | ✅ 200 |
| `GET /api/veiculos/cliente/{id-do-B}` | 403 | ✅ 403 |
| `POST /api/ordens-servico` | 403 | ✅ 403 |
| `GET /api/dashboard` | 403 | ✅ 403 |

### Keycloak (ambiente Docker)
- **URL:** `http://localhost:9080`
- **Usuário admin:** `admin` / `admin`
- **Realm:** `autopecas` (importado automaticamente via `keycloak/realm-export.json`)
- **Issuer URI (validação do token):** `http://localhost:9080/realms/autopecas`
- **JWKS (rede interna do Compose):** `http://keycloak:8080/realms/autopecas/protocol/openid-connect/certs`

---

## 🔑 Autenticação da API

### Obter um token de acesso

Execute o comando abaixo para autenticar com o usuário importado:

```bash
curl -X POST "http://localhost:9080/realms/autopecas/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=autopecas-api" \
  -d "username=test_user" \
  -d "password=1234"
```

Se as credenciais estiverem corretas, a resposta será semelhante a:

```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIs...",
  "expires_in": 300,
  "refresh_expires_in": 1800,
  "refresh_token": "...",
  "token_type": "Bearer"
}
```

Copie o valor de `access_token`.

### Utilizando o token

Inclua o token no cabeçalho `Authorization` das requisições:

```http
Authorization: Bearer <access_token>
```

Exemplo com `curl`:

```bash
curl -X GET "http://localhost:8080/api/clientes" \
  -H "Authorization: Bearer <access_token>"
```

### Autenticação pelo Swagger

1. Acesse `http://localhost:8080/swagger-ui.html`.
2. Clique no botão **Authorize**.
3. Informe o token no formato:

```text
Bearer <access_token>
```

4. Clique em **Authorize** e feche a janela.

A partir desse momento, todas as requisições realizadas pelo Swagger incluirão automaticamente o token JWT.

---

## 🌐 Endpoints da API

A documentação interativa completa está disponível via Swagger após subir a aplicação:

> **Swagger UI:** `http://localhost:8080/swagger-ui.html`  
> **OpenAPI JSON:** `http://localhost:8080/v3/api-docs`

### Clientes — `/api/clientes`

| Método   | Endpoint                        | Roles                            | Descrição                          |
|----------|---------------------------------|----------------------------------|------------------------------------|
| `GET`    | `/api/clientes`                 | `ADMIN`, `ATENDENTE`             | Lista todos os clientes            |
| `GET`    | `/api/clientes/{id}`            | `ADMIN`, `ATENDENTE`, `CLIENTE`† | Busca cliente por ID               |
| `GET`    | `/api/clientes/buscarDOC?documento=` | `ADMIN`, `ATENDENTE`        | Busca por CPF/CNPJ                 |
| `POST`   | `/api/clientes/pf`              | `ADMIN`, `ATENDENTE`             | Cadastra cliente Pessoa Física     |
| `POST`   | `/api/clientes/pj`              | `ADMIN`, `ATENDENTE`             | Cadastra cliente Pessoa Jurídica   |
| `PUT`    | `/api/clientes/{id}`            | `ADMIN`, `ATENDENTE`             | Atualiza dados do cliente          |
| `DELETE` | `/api/clientes/{id}`            | `ADMIN`                          | Desativa cliente                   |

† Somente o próprio cadastro do `CLIENTE` autenticado.

### Veículos — `/api/veiculos`

| Método   | Endpoint                             | Roles                            | Descrição                          |
|----------|--------------------------------------|----------------------------------|------------------------------------|
| `GET`    | `/api/veiculos`                      | `ADMIN`, `ATENDENTE`             | Lista todos os veículos            |
| `GET`    | `/api/veiculos/{id}`                 | `ADMIN`, `ATENDENTE`, `CLIENTE`† | Busca veículo por ID               |
| `GET`    | `/api/veiculos/placa/{placa}`        | `ADMIN`, `ATENDENTE`             | Busca veículo por placa            |
| `GET`    | `/api/veiculos/cliente/{clienteId}`  | `ADMIN`, `ATENDENTE`, `CLIENTE`† | Lista veículos de um cliente       |
| `POST`   | `/api/veiculos`                      | `ADMIN`, `ATENDENTE`             | Cadastra veículo                   |
| `PUT`    | `/api/veiculos/{id}`                 | `ADMIN`, `ATENDENTE`             | Atualiza veículo                   |
| `DELETE` | `/api/veiculos/{id}`                 | `ADMIN`                          | Remove veículo                     |

† Somente veículos do próprio `CLIENTE` autenticado.

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

| Método  | Endpoint                                | Roles                                          | Descrição                                                    |
|---------|-----------------------------------------|------------------------------------------------|--------------------------------------------------------------|
| `GET`   | `/api/ordens-servico`                   | `ADMIN`, `ATENDENTE`, `MECANICO`, `CLIENTE`†  | Lista OS (filtros: `status`, `clienteId`, `mecanicoId`, paginação) |
| `GET`   | `/api/ordens-servico/{numero}`          | `ADMIN`, `ATENDENTE`, `MECANICO`, `CLIENTE`†  | Busca OS pelo número                                         |
| `POST`  | `/api/ordens-servico`                   | `ADMIN`, `ATENDENTE`                           | Cria nova OS                                                 |
| `PATCH` | `/api/ordens-servico/{id}/status`       | `ADMIN`, `ATENDENTE`, `MECANICO`               | Avança o status da OS                                        |
| `PATCH` | `/api/ordens-servico/{id}/diagnostico`  | `ADMIN`, `MECANICO`                            | Registra diagnóstico na OS                                   |
| `PATCH` | `/api/ordens-servico/{id}/mecanico`     | `ADMIN`, `ATENDENTE`                           | Atribui mecânico à OS                                        |

† O `CLIENTE` só acessa a OS que é dele: na listagem, apenas com `clienteId` igual ao seu; na
busca por número, apenas se a OS for do seu cadastro.

#### Abertura com serviços e peças

`POST /api/ordens-servico` aceita, além dos dados de recepção, os serviços e peças já acordados
no balcão. Ambos são opcionais e entram ao **preço vigente no catálogo**, copiado no momento da
abertura — uma alteração posterior de preço não muda retroativamente o que foi combinado.

```json
{
  "clienteId": "…", "veiculoId": "…",
  "queixaCliente": "Barulho no motor ao acelerar",
  "observacoesEntrada": "Veículo entrou pela manhã",
  "quilometragemEntrada": 45000,
  "itensServico": [{ "servicoId": "…", "quantidade": 2 }],
  "itensPeca":    [{ "pecaId": "…",    "quantidade": 1 }]
}
```

A resposta traz `id` e `numero` (formato `OS-XXXXXX`, de uma sequence do PostgreSQL) — a
identificação única da OS.

> **Os itens da abertura não baixam estoque.** A baixa continua acontecendo na aprovação do
> orçamento, que é quando a peça é de fato comprometida. Debitar nos dois momentos contaria a
> mesma saída duas vezes.

#### Listagem: a fila de trabalho

`GET /api/ordens-servico` **sem** o parâmetro `status` devolve a fila operacional da oficina:

- **Exclui logicamente** as OS `FINALIZADA` e `ENTREGUE` — elas continuam no banco e seguem
  acessíveis por busca direta (`GET /api/ordens-servico/{numero}`) ou por filtro explícito.
- **Ordena por urgência:** `EM_EXECUCAO` → `AGUARDANDO_APROVACAO` → `EM_DIAGNOSTICO` →
  `RECEBIDA`, e dentro de cada faixa as **mais antigas primeiro**.
- A ordenação é fixa: o parâmetro `sort` é ignorado nesse modo. A prioridade da oficina é regra
  de negócio, não preferência de quem chama.

Informar `status` é opt-in explícito e devolve exatamente aquele status — encerrado ou não —
aí sim respeitando o `sort` da requisição. `clienteId` e `mecanicoId` apenas estreitam o recorte.

A regra vive em `StatusOS.prioridadeNaFila()` / `emAndamento()`; a query JPQL a materializa num
`CASE`, e `StatusOSFilaTest` compara os dois lados para que não divirjam silenciosamente.

### Orçamentos — `/api/ordens-servico/{osId}/orcamentos`

| Método  | Endpoint                                               | Roles                            | Descrição                    |
|---------|--------------------------------------------------------|----------------------------------|------------------------------|
| `POST`  | `/api/ordens-servico/{osId}/orcamentos`                | `ADMIN`, `ATENDENTE`              | Cria orçamento para a OS     |
| `GET`   | `/api/ordens-servico/{osId}/orcamentos`                | `ADMIN`, `ATENDENTE`, `CLIENTE`† | Lista orçamentos da OS       |
| `PATCH` | `/api/ordens-servico/{osId}/orcamentos/{id}/enviar`    | `ADMIN`, `ATENDENTE`              | Envia orçamento ao cliente   |
| `PATCH` | `/api/ordens-servico/{osId}/orcamentos/{id}/aprovar`   | `ADMIN`, `ATENDENTE`, `CLIENTE`† | Aprova orçamento             |
| `PATCH` | `/api/ordens-servico/{osId}/orcamentos/{id}/rejeitar`  | `ADMIN`, `ATENDENTE`, `CLIENTE`† | Rejeita orçamento            |

† Somente o `CLIENTE` dono da OS informada em `{osId}`.

### Notificação de status por e-mail

Todo avanço de status via `PATCH /api/ordens-servico/{id}/status` dispara um aviso ao cliente,
desde que ele tenha `email` cadastrado e `aceitaNotificacoes = true`.

O canal é uma outbound port (`NotificadorDeStatusOS`), então trocar e-mail por SMS ou push não
toca no caso de uso. Duas garantias no contrato:

- **A notificação nunca derruba a operação.** Ela é enviada **fora da transação**, depois do
  commit, e o adapter registra falhas em log em vez de propagar. Um servidor SMTP fora do ar não
  pode reverter um avanço de status que a oficina já executou no mundo real.
- **Sem SMTP configurado, a aplicação sobe normalmente.** O Spring Boot só autoconfigura o
  `JavaMailSender` quando `spring.mail.host` existe; sem ele o adapter apenas registra o aviso em
  log. É o comportamento padrão no Compose e nos testes.

Para ativar o envio real:

```bash
SPRING_MAIL_HOST=smtp.exemplo.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=usuario
SPRING_MAIL_PASSWORD=senha
SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH=true
SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE=true
MAIL_REMETENTE=nao-responda@suaoficina.com
```

### Dashboard — `/api/dashboard`

| Método | Endpoint                               | Roles   | Descrição                            |
|--------|----------------------------------------|---------|--------------------------------------|
| `GET`  | `/api/dashboard`                       | `ADMIN` | Visão geral (KPIs da oficina)        |
| `GET`  | `/api/dashboard/tempo-medio-execucao`  | `ADMIN` | Tempo médio de execução por serviço  |

---

## 🐳 Infraestrutura e Execução

### Visão geral

```
                        máquina host
  ┌──────────────────────────────────────────────────────────────┐
  │  localhost:8080 ──┐                    localhost:9080 ──┐    │
  └───────────────────┼───────────────────────────────────┼──────┘
                      │  (extraPortMapping do kind)       │
  ┌───────────────────▼───────────────────────────────────▼──────┐
  │  cluster kind "autopecas"          1 control-plane + 2 workers│
  │                                                               │
  │  ┌──────────────── namespace: autopecas ────────────────────┐ │
  │  │                                                          │ │
  │  │   NodePort 30080          NodePort 30081                 │ │
  │  │        │                       │                         │ │
  │  │        ▼                       ▼                         │ │
  │  │  ┌───────────┐  ◄── HPA   ┌──────────┐                   │ │
  │  │  │autopecas- │  2..5      │ keycloak │  (1 réplica)      │ │
  │  │  │   api     │  réplicas  │          │                   │ │
  │  │  └─────┬─────┘            └────┬─────┘                   │ │
  │  │        │      ┌────────────────┘                         │ │
  │  │        ▼      ▼                                          │ │
  │  │  ┌──────────────────┐                                    │ │
  │  │  │ postgres (STS)   │  app_db + keycloak_db              │ │
  │  │  │   └── PVC 2Gi    │                                    │ │
  │  │  └──────────────────┘                                    │ │
  │  └──────────────────────────────────────────────────────────┘ │
  │  kube-system: metrics-server  ──alimenta──►  HPA              │
  └───────────────────────────────────────────────────────────────┘
```

### Divisão de responsabilidades

A fronteira entre Terraform e manifestos segue o que muda em que ritmo:

| Camada | O que provisiona | Por quê |
|---|---|---|
| **Terraform** (`infra/terraform/`) | Cluster, namespace, PostgreSQL, metrics-server, ConfigMap do realm | Infraestrutura de base — muda raramente e precisa existir antes da aplicação |
| **Manifestos** (`infra/k8s/`) | Deployments, Services, ConfigMap, Secrets, HPA, PDB | Aplicação — muda a cada release, aplicado por `kubectl` ou por um pipeline |

Essa separação é o que permite dar `kubectl apply` cem vezes ao dia sem tocar no Terraform.

> **Sobreposição intencional:** o namespace é declarado nos dois lados. O Terraform precisa
> dele para criar o banco; os manifestos precisam dele para funcionar num cluster que não veio
> deste Terraform. Aplicar ambos é idempotente.

---

### Pré-requisitos

| Ferramenta | Versão | Necessário para |
|---|---|---|
| Docker Engine | 24+ | Tudo — o kind roda os nós como containers |
| Docker Compose | v2 | Ambiente local sem Kubernetes |
| Terraform | 1.5+ | Provisionar o cluster e o banco |
| kubectl | 1.29+ | Aplicar os manifestos |
| kind | 0.23+ | Instalado pelo provider Terraform, mas o CLI é usado no `kind load` |

```bash
# Windows (winget)
winget install Hashicorp.Terraform Kubernetes.kubectl Kubernetes.kind

# macOS (brew)
brew install terraform kubectl kind
```

#### Sem instalar nada: Terraform via container

Terraform e kind podem rodar em container, com o Docker do host. Útil em máquina onde não se
quer instalar as ferramentas — foi assim que este ambiente foi validado.

O provider kind **executa o binário `docker`** (não fala com a API diretamente), então a imagem
precisa do docker CLI e do socket montado:

```bash
# Imagem auxiliar: terraform + docker CLI
cat > Dockerfile.tf <<EOF
FROM hashicorp/terraform:1.9
RUN apk add --no-cache docker-cli kubectl bash
ENTRYPOINT []
EOF
docker build -f Dockerfile.tf -t tf-runner:local .

# 1ª etapa — criar o cluster (não precisa dos providers kubernetes/helm)
docker run --rm -v /var/run/docker.sock:/var/run/docker.sock -v "$PWD:/repo"   -w /repo/infra/terraform tf-runner:local   sh -c "terraform init && terraform apply -auto-approve -target=kind_cluster.autopecas"
```

A 2ª etapa tem um detalhe: o kubeconfig escrito pelo kind aponta para `127.0.0.1:<porta>`, que
**dentro de um container é o próprio container**, não o host. Gere uma variante apontando para
`host.docker.internal` e passe-a em `kubeconfig_path_override`:

```bash
sed -e "s#server: https://127.0.0.1:#server: https://host.docker.internal:#"     -e "s#certificate-authority-data:.*#insecure-skip-tls-verify: true#"     infra/terraform/autopecas-config > /tmp/kubeconfig-container.yaml

docker run --rm -v /var/run/docker.sock:/var/run/docker.sock -v "$PWD:/repo"   -v /tmp/kubeconfig-container.yaml:/kube/config:ro   -w /repo/infra/terraform tf-runner:local   sh -c "terraform apply -auto-approve -var kubeconfig_path_override=/kube/config"
```

O `insecure-skip-tls-verify` é aceitável aqui porque o certificado do API server não inclui
`host.docker.internal` nos SANs, e o tráfego não sai da máquina.

#### Carregar a imagem sem o CLI do kind

`kind load` apenas importa a imagem no containerd de cada nó — o que se faz direto:

```bash
docker save autopecas-api:local -o /tmp/app-image.tar
for n in autopecas-control-plane autopecas-worker autopecas-worker2; do
  docker exec -i "$n" ctr --namespace=k8s.io images import --all-platforms - < /tmp/app-image.tar
done
```

> Envie o tar por **stdin**, não via `docker cp` para `/tmp`: nos nós kind o `/tmp` é um tmpfs
> que sombreia o arquivo copiado — o `cp` retorna sucesso e o arquivo não aparece.

#### Memória necessária

Reserve **~8 GB de RAM** para o Docker. O consumo medido, em regime permanente:

| Componente | Memória |
|---|---|
| API (por réplica) | ~420 Mi |
| Keycloak | ~543 Mi |
| PostgreSQL | ~79 Mi |
| 3 nós kind + control-plane | ~1,5 GB |

Com o HPA em `maxReplicas: 5`, o pico fica em torno de 5 GB. **Não aumente `maxReplicas` em
cluster local**: durante os testes, 6 réplicas somadas ao Keycloak saturaram a máquina, e no
reinício seguinte o boot simultâneo de todos os pods travou o próprio engine do Docker.

---

### Containerização (Docker)

#### Dockerfile

Build em três estágios:

1. **build** — `maven:3.9-eclipse-temurin-21` compila o JAR. O `pom.xml` é copiado antes do
   `src/` para que alterar código não invalide a camada de download das dependências.
2. **layers** — explode o JAR com `-Djarmode=tools extract --layers`, separando bibliotecas
   (mudam pouco) do código da aplicação (muda sempre). Sem isso, cada `docker push` reenviaria
   ~60 MB de dependências.
3. **runtime** — `eclipse-temurin:21-jre`, usuário não-root `appuser`, camadas copiadas na
   ordem da que menos muda para a que mais muda.

Flags da JVM e o motivo de cada uma:

| Flag | Motivo |
|---|---|
| `-XX:+UseContainerSupport` | A JVM lê os limites do cgroup em vez da memória da máquina |
| `-XX:MaxRAMPercentage=75` | Heap proporcional ao `limits.memory` do pod |
| `-XX:+ExitOnOutOfMemoryError` | No OOM o processo morre e o orquestrador reinicia, em vez de ficar de pé e degradado |
| `-Duser.timezone=UTC` | Alinha o `@CreationTimestamp` do Hibernate (fuso da JVM) com o `Relogio` da aplicação |

```bash
docker build -t autopecas-api:local .
```

#### docker-compose (desenvolvimento local)

```bash
docker compose up -d --build          # postgres + keycloak + app
docker compose --profile qa up -d     # sobe também o SonarQube
docker compose logs -f app
docker compose down                   # -v também apaga os volumes
```

| Serviço | URL | Observação |
|---|---|---|
| API | http://localhost:8080 | Swagger em `/swagger-ui.html` |
| Keycloak | http://localhost:9080 | `admin` / `admin` |
| SonarQube | http://localhost:9000 | Só com `--profile qa` |

Duas mudanças em relação à versão anterior do Compose:

- **SonarQube atrás de um profile.** Antes, todo `up` levantava dois bancos e uma JVM extra de
  ~2 GB só para ter o Sonar disponível — que só é necessário ao rodar a análise.
- **Banco do Keycloak separado.** Os dois compartilhavam `app_db`. Como a aplicação agora roda
  em `prod` com `ddl-auto: validate`, o Hibernate passaria a inspecionar um schema cheio de
  tabelas do Keycloak. O script `docker/postgres/init-keycloak-db.sh` cria `keycloak_db` no
  primeiro boot do volume.

> Se você já tinha o volume `postgres_data` da versão anterior, o script de init **não roda**
> (ele só executa em volume vazio). Rode `docker compose down -v` para recriar.

---

### Provisionamento (Terraform)

```bash
cd infra/terraform

terraform init

# Primeiro apply em duas etapas — ver nota abaixo
terraform apply -target=kind_cluster.autopecas
terraform apply
```

> **Por que duas etapas na primeira vez:** os providers `kubernetes` e `helm` são configurados
> a partir do kubeconfig que o `kind_cluster` escreve. Terraform precisa resolver a configuração
> do provider durante o *plan*, quando o arquivo ainda não existe. É uma limitação conhecida de
> providers configurados a partir de recursos do mesmo plano. Applies seguintes rodam em um
> comando só.

#### Variáveis principais

| Variável | Default | Descrição |
|---|---|---|
| `cluster_name` | `autopecas` | Nome do cluster kind |
| `node_image` | `kindest/node:v1.31.0` | Fixa a versão do Kubernetes |
| `worker_count` | `2` | Nós worker (1–5) |
| `app_host_port` | `8080` | Porta do host → NodePort 30080 |
| `keycloak_host_port` | `9080` | Porta do host → NodePort 30081 |
| `postgres_storage` | `2Gi` | Volume do banco |
| `postgres_password` | `postgres` | Prefira `TF_VAR_postgres_password` |
| `install_metrics_server` | `true` | Desligue só se o cluster já tiver um |

Copie `terraform.tfvars.example` para `terraform.tfvars` para sobrescrever.

---

### Recursos criados pelo Terraform

| Arquivo | Recurso | O que é |
|---|---|---|
| `cluster.tf` | `kind_cluster.autopecas` | Cluster com 1 control-plane + N workers. Mapeia as portas 30080/30081 do nó para 8080/9080 do host |
| `cluster.tf` | `kubernetes_namespace.autopecas` | Namespace `autopecas` com Pod Security Standards em `baseline` |
| `database.tf` | `kubernetes_secret.postgres` | Usuário, senha e nome do banco |
| `database.tf` | `kubernetes_config_map.postgres_init` | Script que cria `keycloak_db` no primeiro boot |
| `database.tf` | `kubernetes_stateful_set.postgres` | PostgreSQL 16 com PVC de 2 Gi e probes `pg_isready` |
| `database.tf` | `kubernetes_service.postgres` | Service headless na porta 5432 |
| `keycloak-realm.tf` | `kubernetes_config_map.keycloak_realm` | Realm importado no boot do Keycloak |
| `metrics-server.tf` | `helm_release.metrics_server` | metrics-server no `kube-system` — **pré-requisito do HPA** |

**Por que StatefulSet e não Deployment para o banco:** o Postgres tem identidade e disco. O
StatefulSet garante nome estável (`postgres-0`) e liga o pod sempre ao mesmo PVC; um Deployment
poderia recriar o pod apontando para outro volume.

**Por que o realm fica no Terraform:** o `realm-export.json` tem ~70 KB e é o mesmo arquivo que
o Compose monta. Lido com `file()`, Compose e Kubernetes consomem a mesma fonte e não divergem.
O kustomize não serviria porque se recusa a ler arquivos fora do diretório da kustomization.

---

### Orquestração (Kubernetes)

```bash
# 1. A imagem precisa estar DENTRO do cluster — kind não usa registry
kind load docker-image autopecas-api:local --name autopecas

# 2. Aplicar
kubectl apply -f infra/k8s/
```

Se estiver aplicando num cluster que não veio deste Terraform, crie antes o ConfigMap do realm:

```bash
kubectl create configmap keycloak-realm -n autopecas \
  --from-file=realm-export.json=keycloak/realm-export.json
```

---

### Recursos criados pelos manifestos

| Arquivo | Recursos | Pontos de atenção |
|---|---|---|
| `00-namespace.yaml` | Namespace | Pod Security Standards: `baseline` |
| `10-configmap-app.yaml` | ConfigMap `autopecas-config` | Endereços e perfil — nada sensível |
| `11-secret-app.yaml` | Secrets `autopecas-secrets`, `keycloak-secrets` | Credenciais. **Valores de desenvolvimento** |
| `20-keycloak.yaml` | Deployment + Service + NodePort | `KC_HOSTNAME_URL` fixa o issuer |
| `30-app-deployment.yaml` | Deployment + Service + NodePort + PDB | Probes, `requests`/`limits`, rootfs somente leitura |
| `40-app-hpa.yaml` | HorizontalPodAutoscaler | CPU 70% (motor) / memória 85% (proteção), 2–5 réplicas |

#### ConfigMap × Secret

O critério: **se o valor pode aparecer num `kubectl describe` sem causar dano, é ConfigMap.**

| ConfigMap | Secret |
|---|---|
| `SPRING_PROFILES_ACTIVE`, `DB_URL` | `DB_USERNAME`, `DB_PASSWORD` |
| `OAUTH2_JWK_URI`, `OAUTH2_ISSUER_URI` | `KEYCLOAK_ADMIN`, `KEYCLOAK_ADMIN_PASSWORD` |
| `KEYCLOAK_REALM`, `KEYCLOAK_CLIENT_ID`, `TZ` | `KC_DB_USERNAME`, `KC_DB_PASSWORD` |

Ambos entram no pod por `envFrom`, então as chaves são exatamente os nomes das variáveis lidas
por `application-prod.yml`.

#### Probes

| Probe | Endpoint | Papel |
|---|---|---|
| `startupProbe` | `/actuator/health/readiness` | Dá até ~3min30 para Flyway + JPA subirem, sem afrouxar a liveness |
| `readinessProbe` | `/actuator/health/readiness` | Tira o pod do balanceamento se ele não consegue atender |
| `livenessProbe` | `/actuator/health/liveness` | Reinicia o pod travado |

A distinção importa: apontar a **liveness** para um endpoint que depende do banco causaria
reinícios em cascata numa indisponibilidade do PostgreSQL — os pods seriam mortos justamente
quando não há nada de errado com eles.

`/actuator/health/**` é liberado sem autenticação no `SecurityConfig`: o kubelet chama as probes
sem credencial alguma, e exigir token faria toda readiness/liveness responder 401 — pod em
`CrashLoopBackOff` com a aplicação perfeitamente saudável. O que fica exposto é só `UP`/`DOWN`;
o profile `prod` publica apenas `health` e `info`, com `show-details: never`. Os demais endpoints
do Actuator seguem exigindo token — verificado: `/actuator/env` responde 401.

#### Por que a API tem limite de memória mas não de CPU

Throttling de CPU numa JVM degrada muito a latência (o GC e o JIT competem pelas mesmas fatias),
e o `requests.cpu` já garante a fatia mínima sob contenção. Memória tem limite porque estouro
precisa virar OOMKill: um pod vazando memória deve morrer e ser substituído, não arrastar o nó.

---

### Passo a passo completo

Do zero até a API respondendo autenticada:

```bash
# 1 ─ Provisionar cluster + banco
cd infra/terraform
terraform init
terraform apply -target=kind_cluster.autopecas -auto-approve
terraform apply -auto-approve
cd ../..

# 2 ─ Construir e carregar a imagem
docker build -t autopecas-api:local .
kind load docker-image autopecas-api:local --name autopecas

# 3 ─ Aplicar os manifestos
kubectl apply -f infra/k8s/

# 4 ─ Aguardar (o Keycloak importa o realm no primeiro boot; leva ~1min)
kubectl wait --for=condition=available --timeout=300s \
  deployment/keycloak deployment/autopecas-api -n autopecas

# 5 ─ Conferir
kubectl get pods,svc,hpa -n autopecas
```

#### Obter um token e chamar a API

```bash
TOKEN=$(curl -s -X POST \
  "http://localhost:9080/realms/autopecas/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=autopecas-api" \
  -d "username=test_user" \
  -d "password=1234" | sed -E 's/.*"access_token":"([^"]+)".*/\1/')

curl -s http://localhost:8080/api/clientes -H "Authorization: Bearer $TOKEN"
```

> **Sobre o issuer:** o Keycloak roda com `KC_HOSTNAME_URL=http://keycloak:8080`, então o claim
> `iss` do token é sempre esse endereço — mesmo quando o token é pedido em `localhost:9080`. É
> o que permite à API validar com `OAUTH2_ISSUER_URI=http://keycloak:8080/realms/autopecas`.
> Sem fixar o hostname, o issuer refletiria o `Host` da requisição e o token seria rejeitado.

---

### Autoscaling: como verificar

```bash
kubectl get hpa -n autopecas -w
```

```
NAME            REFERENCE                  TARGETS                        MINPODS  MAXPODS  REPLICAS
autopecas-api   Deployment/autopecas-api   cpu: 2%/70%, memory: 55%/85%   2        5        2
```

Se `TARGETS` mostrar `<unknown>`, o metrics-server não está coletando:

```bash
kubectl get deployment metrics-server -n kube-system
kubectl top pods -n autopecas          # precisa responder
```

#### Gerar carga

```bash
kubectl run carga --rm -it --restart=Never --image=busybox:1.36 -n autopecas -- \
  sh -c 'while true; do wget -q -O- http://autopecas-api:8080/actuator/health >/dev/null; done'
```

Com `stabilizationWindowSeconds: 60` na subida, novas réplicas aparecem em ~1 minuto. Ao parar
a carga, a descida leva 5 minutos — janela propositalmente longa para evitar o vai-e-vem de
réplicas quando a carga oscila, já que cada pod novo custa o startup da JVM.

#### O que a medição mostrou

Execução real neste projeto, em cluster kind. Ciclo completo de subida e descida,
lido dos eventos do HPA:

```
SuccessfulRescale  New size: 4   reason: cpu resource utilization above target
SuccessfulRescale  New size: 3   reason: All metrics below target
SuccessfulRescale  New size: 2   reason: All metrics below target
```

| Métrica | Ocioso | Sob carga |
|---|---|---|
| CPU | 2% do alvo | **220%** do alvo → subiu 2 → 4 réplicas |
| Memória | 52% do alvo | 51% do alvo (praticamente inalterada) |

Repare no contraste: sob carga a CPU vai a 220% enquanto a memória **cai** de 52% para 51%. É a
demonstração direta de que, nesta aplicação, memória não é sinal de demanda — e a razão de ela
ser configurada como proteção, não como motor.

A subida respeitou a política de 2 pods por janela (2 → 4, não 2 → 8), e a descida levou os
5 minutos da janela de estabilização, passando por 3 antes de voltar ao piso de 2.

Na configuração anterior — request de 512 Mi e alvo de memória em 80% — a mesma carga levou a
frota a **6 réplicas**, com a memória travada em 82% (acima do alvo) em regime permanente.

Dois aprendizados que estão embutidos na configuração atual:

**1. Memória não é sinal de autoscaling para JVM.** O consumo não acompanha a carga — o heap é
gerido pelo GC, não por requisição. E, diferente da CPU, ela **não converge**: acrescentar
réplicas não baixa a memória por pod, porque cada réplica nova carrega o próprio heap. Com o
request original de 512 Mi, os 419 Mi davam 82% de utilização — acima do alvo de 80% — e o HPA
teria escalado até o teto sem nunca voltar. Por isso o request subiu para 768 Mi (patamar normal
~55%) e o alvo de memória foi para 85%: ela virou grade de proteção contra pressão real, e quem
modula a frota é a CPU.

**2. `maxReplicas` precisa caber na máquina.** Cada réplica é uma JVM de ~420 Mi. Seis delas,
somadas ao Keycloak, saturaram o ambiente de teste — e no reinício seguinte o boot simultâneo
de todos os pods travou o engine do Docker. Daí o teto de 5 em cluster local.

---

### Segredos em produção

`infra/k8s/11-secret-app.yaml` está versionado **para o ambiente local**, com valores de
desenvolvimento. Um Secret do Kubernetes é apenas base64: qualquer um com leitura no namespace
lê o conteúdo em claro.

Em produção, escolha um destes caminhos e **remova o arquivo do repositório**:

| Abordagem | Como funciona | Quando usar |
|---|---|---|
| **External Secrets Operator** | O Secret é sincronizado de um cofre (AWS Secrets Manager, Vault, Key Vault). Nada sensível é versionado | Padrão quando já existe um cofre |
| **Sealed Secrets** (Bitnami) | O valor é cifrado com a chave pública do cluster; só o controlador decifra. O arquivo cifrado pode ser versionado | GitOps sem cofre externo |
| **`kubectl create secret` no pipeline** | Valores vêm do cofre de segredos do CI | Setup mais simples |

O mesmo vale para o Terraform: `postgres_password` é `sensitive`, o que impede o valor de
aparecer no output — mas ele **fica em texto claro no arquivo de state**. Por isso o state
nunca deve ser versionado (ver `infra/terraform/.gitignore`); em equipe, use backend remoto com
criptografia (S3 + KMS, Terraform Cloud).

---

### Operação e diagnóstico

```bash
# Logs
kubectl logs -f deployment/autopecas-api -n autopecas
kubectl logs -f deployment/keycloak -n autopecas

# Por que um pod não sobe
kubectl describe pod -l app.kubernetes.io/name=autopecas-api -n autopecas
kubectl get events -n autopecas --sort-by=.lastTimestamp

# Acesso direto ao banco
kubectl exec -it postgres-0 -n autopecas -- psql -U postgres -d app_db

# Forçar novo deploy após rebuild da imagem
docker build -t autopecas-api:local .
kind load docker-image autopecas-api:local --name autopecas
kubectl rollout restart deployment/autopecas-api -n autopecas
kubectl rollout status  deployment/autopecas-api -n autopecas
```

#### Problemas comuns

| Sintoma | Causa provável | Solução |
|---|---|---|
| `ErrImagePull` / `ImagePullBackOff` | Imagem não carregada no kind | `kind load docker-image autopecas-api:local --name autopecas` |
| HPA com `TARGETS: <unknown>` | metrics-server ausente ou sem coletar | `kubectl get deploy metrics-server -n kube-system` |
| API em `CrashLoopBackOff` | Banco indisponível ou migration falhando | `kubectl logs` — procure erro do Flyway |
| 401 em toda requisição | Issuer do token ≠ `OAUTH2_ISSUER_URI` | Confira `KC_HOSTNAME_URL` no Deployment do Keycloak |
| Keycloak reiniciando | Banco `keycloak_db` inexistente | Confira o ConfigMap `postgres-init`; recrie o PVC |
| Porta 8080 ocupada no host | Outro processo | Ajuste `app_host_port` no `terraform.tfvars` |

---

### Destruir o ambiente

```bash
cd infra/terraform
terraform destroy
```

Isso remove o cluster inteiro — e com ele o PVC e todos os dados. Para apagar apenas a
aplicação, mantendo cluster e banco:

```bash
kubectl delete -f infra/k8s/
```

Para zerar o banco sem destruir o cluster (o PVC sobrevive à exclusão do StatefulSet, por
design):

```bash
kubectl delete pvc data-postgres-0 -n autopecas
terraform apply
```

### Executar fora de container (desenvolvimento)

Para iterar no código sem reconstruir a imagem a cada mudança: sobe só as dependências no
Compose e roda a aplicação na máquina, com o profile `dev`.

```bash
docker compose up postgres keycloak -d
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

O profile `dev` difere do `prod` em dois pontos: `ddl-auto: none` (não valida o schema contra
as entidades) e log SQL ligado.

---

## 🔄 CI/CD Local (Self-hosted Runner)

O pipeline de CI/CD está definido em `.github/workflows/ci-cd-local.yml` e é composto por três
jobs executados em sequência:

### Como funciona o pipeline

```
Push / Pull Request
        │
        ├──► build-test          (GitHub-hosted: ubuntu-latest)
        │    ├── Checkout
        │    ├── Java 21 + Maven
        │    └── ./mvnw clean verify  (testes + JaCoCo + ArchUnit)
        │
        ├──► terraform-validation  (GitHub-hosted: ubuntu-latest)
        │    ├── terraform init -backend=false
        │    ├── terraform fmt -check
        │    └── terraform validate
        │
        └──► deploy-local          (self-hosted: linux/x64/autopecas)
             ├── Verificar ferramentas (java, docker, kind, kubectl, terraform)
             ├── Criar/atualizar cluster Kind via Terraform
             ├── docker build → kind load
             ├── kubectl apply -f infra/k8s/
             ├── Aguardar PostgreSQL, Keycloak e API
             └── Health check em /actuator/health/readiness
```

- Os jobs `build-test` e `terraform-validation` rodam nos runners do GitHub e **não precisam de
  nenhuma configuração local**.
- O job `deploy-local` roda **somente em `push`** (nunca em Pull Request) e requer o
  self-hosted runner instalado na máquina.

---

### Pré-requisitos da máquina

Antes de registrar o runner, certifique-se de que as seguintes ferramentas estão instaladas e
disponíveis no `PATH` do usuário que executará o runner:

| Ferramenta     | Versão mínima | Verificar              |
|----------------|---------------|------------------------|
| Java (JDK)     | 21            | `java -version`        |
| Docker Engine  | 24+           | `docker version`       |
| Kind           | 0.23+         | `kind version`         |
| kubectl        | 1.29+         | `kubectl version --client` |
| Terraform      | 1.5+          | `terraform version`    |

> **WSL2 (Windows):** todas as ferramentas devem estar instaladas **dentro do WSL**, não no
> Windows host. O runner roda no Linux do WSL2 e o Docker precisa estar acessível sem `sudo`
> (adicione o usuário ao grupo `docker`: `sudo usermod -aG docker $USER`).

---

### Instalar o GitHub Actions Runner

O script `install-runner.sh` na raiz do projeto baixa e extrai o runner na versão correta:

```bash
chmod +x install-runner.sh
./install-runner.sh
```

Isso cria `~/actions-runner/` com o binário do runner v2.336.0.

---

### Registrar o runner no repositório

1. Acesse o repositório no GitHub → **Settings → Actions → Runners → New self-hosted runner**
2. Copie o token exibido (válido por alguns minutos)
3. Execute o comando de configuração:

```bash
cd ~/actions-runner
./config.sh \
  --url https://github.com/16soat-fiap/16SOAT-TechChallenge1 \
  --token SEU_TOKEN_AQUI \
  --labels autopecas \
  --name meu-runner-local \
  --unattended
```

> O label `autopecas` é **obrigatório** — o workflow seleciona o runner por
> `runs-on: [self-hosted, linux, x64, autopecas]`.

---

### Iniciar o runner

#### Em primeiro plano (para testar)

```bash
cd ~/actions-runner
./run.sh
```

#### Como serviço systemd (para uso contínuo)

```bash
cd ~/actions-runner
sudo ./svc.sh install
sudo ./svc.sh start
sudo ./svc.sh status
```

Para parar ou desinstalar:

```bash
sudo ./svc.sh stop
sudo ./svc.sh uninstall
```

---

### Secret necessário

O job `deploy-local` usa o secret `POSTGRES_PASSWORD` na etapa do Terraform:

```yaml
env:
  TF_VAR_postgres_password: ${{ secrets.POSTGRES_PASSWORD }}
```

Cadastre-o em **Settings → Secrets and variables → Actions → New repository secret**:

| Secret             | Valor sugerido (local) | Descrição                     |
|--------------------|------------------------|-------------------------------|
| `POSTGRES_PASSWORD`| `postgres`             | Senha do PostgreSQL no cluster|

---

### Executar o pipeline manualmente

O workflow dispara automaticamente em `push` para `main`, `develop` ou
`refactor/arquitetura-hexagonal`. Para disparar sem commit:

1. Acesse **Actions → CI/CD - Autopecas API - Local Kubernetes**
2. Clique em **Run workflow** → selecione a branch → **Run workflow**

> O job `deploy-local` só é executado em `push` — um `workflow_dispatch` manual também aciona
> o deploy.

---

### Verificar o runner no GitHub

Após `./run.sh` ou o serviço estar ativo, o runner aparece como **Idle** em:

**Settings → Actions → Runners**

Se aparecer como **Offline**, verifique:

| Sintoma | Causa provável | Solução |
|---|---|---|
| Runner Offline | Processo parado | `sudo ./svc.sh status` ou reinicie com `./run.sh` |
| Job ignorado | Label errado | Confirme `--labels autopecas` no `config.sh` |
| `docker: permission denied` | Usuário fora do grupo docker | `sudo usermod -aG docker $USER` e reabrir sessão |
| `kind: command not found` | Kind não está no PATH do runner | Adicione ao PATH em `~/.profile` ou `~/.bashrc` |
| `terraform: command not found` | Terraform não está no PATH | Mesmo que o anterior |
| Deploy não ocorre em PR | Comportamento esperado | O deploy só roda em `push`, não em PR |

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

| Tipo                       | Descrição                                                                            |
|----------------------------|--------------------------------------------------------------------------------------|
| **Arquitetura (ArchUnit)** | 10 regras de fronteira: domínio sem framework, aplicação sem Spring, adapters isolados |
| **Domínio**                | Agregados e Value Objects isolados — sem Spring, sem mocks, sem banco. Rodam em ms    |
| **Casos de Uso**           | Dublês das *ports* do projeto (não de interfaces do Spring Data) + fakes de tempo e transação |
| **Integração**             | Testcontainers sobe um PostgreSQL real; o schema vem das **mesmas migrations Flyway** de produção |
| **Contexto**               | `AutopecasApplicationTests` valida que o Spring Context sobe                          |

> O domínio ser cético de tecnologia tem um efeito prático direto nos testes: as regras de
> negócio são verificadas sem subir contexto nem container, e o tempo entra por parâmetro, o que
> permite asserções exatas sobre datas em vez de aproximações.

### Gate de Cobertura (JaCoCo)

A build falha automaticamente se a cobertura de linhas ficar abaixo de **80%**.

Classes excluídas da contagem (sem lógica de negócio):
- `AutopecasApplication` (entry point)
- `adapter/in/web/dto/**` (records de entrada/saída HTTP)
- `adapter/in/web/mapper/**Mapper(Impl).class` (gerados pelo MapStruct)
- `adapter/out/persistence/entity/**` e `projection/**` (estrutura de persistência)
- `application/port/in/view/**` (records de projeção)
- `domain/enums/**`
- `config/**` (wiring declarativo)
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

Usado pelo container em qualquer ambiente — Compose e Kubernetes. Neste profile o Hibernate
roda com `ddl-auto: validate`: quem cria e evolui o schema é o Flyway, e o Hibernate apenas
confere se o mapeamento bate com o banco.

| Variável                   | Onde vive no K8s | Descrição                                              |
|----------------------------|------------------|--------------------------------------------------------|
| `SPRING_PROFILES_ACTIVE`   | ConfigMap        | `prod`                                                 |
| `DB_URL`                   | ConfigMap        | URL JDBC do PostgreSQL                                 |
| `DB_USERNAME`              | **Secret**       | Usuário do banco                                       |
| `DB_PASSWORD`              | **Secret**       | Senha do banco                                         |
| `OAUTH2_ISSUER_URI`        | ConfigMap        | Issuer esperado no claim `iss` do token                |
| `OAUTH2_JWK_URI`           | ConfigMap        | Endpoint JWKS usado para validar a assinatura          |
| `KEYCLOAK_CLIENT_ID`       | ConfigMap        | Client ID no Keycloak (padrão: `autopecas-api`)        |
| `KEYCLOAK_AUTH_SERVER_URL` | ConfigMap        | URL base do Keycloak (documentação/health)             |
| `KEYCLOAK_REALM`           | ConfigMap        | Nome do realm                                          |
| `JAVA_OPTS`                | ConfigMap        | Flags extras da JVM                                    |
| `SPRING_MAIL_HOST`         | ConfigMap        | SMTP das notificações. **Ausente = modo log**          |
| `SPRING_MAIL_PORT`         | ConfigMap        | Porta do SMTP                                          |
| `MAIL_REMETENTE`           | ConfigMap        | Endereço no campo `From`                               |
| `SPRING_MAIL_USERNAME`     | **Secret**       | Usuário do SMTP                                        |
| `SPRING_MAIL_PASSWORD`     | **Secret**       | Senha do SMTP                                          |

O critério da divisão: se o valor pode aparecer num `kubectl describe` sem causar dano, é
ConfigMap. Endereços são endereços; credenciais vão para o Secret.

`OAUTH2_JWK_URI` e `OAUTH2_ISSUER_URI` podem apontar para hosts diferentes — é o caso no
Compose, onde o JWKS é buscado pela rede interna (`keycloak:8080`) e o issuer é o endereço que
o cliente usa (`localhost:9080`).

### Docker Compose (`app` service)

```yaml
environment:
  SPRING_PROFILES_ACTIVE: prod
  DB_URL: jdbc:postgresql://postgres:5432/app_db
  DB_USERNAME: postgres
  DB_PASSWORD: postgres
  OAUTH2_JWK_URI: http://keycloak:8080/realms/autopecas/protocol/openid-connect/certs
  OAUTH2_ISSUER_URI: http://localhost:9080/realms/autopecas
```

---

## 📎 Diagramas

Diagramas de **Domain Storytelling** e **Event Storming**:

🔗 [Abrir no Miro](https://miro.com/app/board/uXjVHTmOiSE=/)

---

## 👥 Equipe

Desenvolvido como parte do **Tech Challenge** da **FIAP — Turma 16SOAT**.
