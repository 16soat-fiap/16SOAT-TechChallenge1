# 16SOAT-TechChallenge1 - Oficina Mecânica - Sistema de Gestão de Ordens de Serviço (MVP)

Sistema Integrado de Atendimento e Execução de Serviços, que permitirá aos clientes acompanhar em tempo real o andamento do serviço, autorizar reparos adicionais via aplicativo e garantir uma gestão interna eficiente e segura.

Este projeto é o Back-end da primeira versão (MVP) do sistema de gestão para uma oficina mecânica de médio porte. O foco principal é a gestão eficiente de ordens de serviço, clientes, veículos e peças, garantindo qualidade e escalabilidade.

## 🛠 Stack Tecnológica

*   **Linguagem:** Java 21 (LTS)
*   **Framework:** Spring Boot 4.0.6
*   **Gerenciador de Dependências:** Maven
*   **Banco de Dados:** PostgreSQL
* **Segurança:** Spring Security com Keycloak
* **Autenticação/Autorização:** Keycloak (Identity and Access Management)
* **Especificações:** Jakarta EE

## 📐 Arquitetura e Design

A aplicação foi desenvolvida seguindo os princípios da **Arquitetura em camadas**  combinado com **Domain-Driven Design (DDD)** para garantir um modelo de domínio rico e isolado.

*   **Modelos Ricos:** Lógica de negócio encapsulada nas entidades de domínio.
*   **Value Objects:** Uso de Java Records para imutabilidade e encapsulamento de atributos.

## 📂 Estrutura do Projeto

- `src/main/java/com/autopecas/autopecas/controller`: endpoints REST
- `src/main/java/com/autopecas/autopecas/service`: regras de aplicação e casos de uso
- `src/main/java/com/autopecas/autopecas/domain`: entidades, enums e objetos de valor do domínio
- `src/test/java`: suíte de testes unitários e de controller/service

## 🚀 Como Executar

### Pré-requisitos
*   JDK 21
*   Maven 3.9+
*   Docker e Docker Compose
*   Keycloak (provisionado via Docker Compose)

### Passos para execução

1.  **Clonar o repositório:**
    ```bash
    git clone <repository-url>
    cd 16SOAT-TechChallenge1
    ```

2. **Configurar o Banco de Dados (via Docker):**
   ```bash
   docker-compose up -d
   ```
   
3. **Compilar e Rodar a Aplicação:**
   ```bash
   ./mvnw spring-boot:run
   ```

## 🧪 Testes

Para executar apenas os testes:
```bash
./mvnw test
```

Para executar o ciclo completo com empacotamento e geração do relatório de cobertura via JaCoCo:
```bash
./mvnw verify
```

> Observação: o `verify` validado neste projeto utiliza a infraestrutura disponível no `docker-compose.yml` e um teste de contexto sobe a aplicação com o profile `dev`.

## 📊 Cobertura de Testes com JaCoCo

O `JaCoCo` está configurado no `pom.xml` para:

- instrumentar a suíte durante os testes (`prepare-agent`)
- gerar o relatório no ciclo `verify` (`report`)
- publicar saídas em HTML, XML e CSV

Arquivos gerados após `./mvnw verify`:

- `target/site/jacoco/index.html`
- `target/site/jacoco/jacoco.xml`
- `target/site/jacoco/jacoco.csv`

### Cobertura real validada

Cobertura extraída de uma execução real de `./mvnw verify` em `2026-06-21`, com `BUILD SUCCESS` e `310` testes executados.

| Pacote | Instruções | Branches | Linhas | Métodos |
| --- | ---: | ---: | ---: | ---: |
| `com.autopecas.autopecas.controller` | 83,74% | 66,67% | 86,79% | 84,09% |
| `com.autopecas.autopecas.service` | 91,73% | 73,19% | 95,79% | 83,84% |
| `com.autopecas.autopecas.domain.entity` | 100,00% | 100,00% | 100,00% | 100,00% |
| `com.autopecas.autopecas.domain.enums` | 100,00% | 100,00%* | 100,00% | 100,00% |

\* Para `domain.enums`, o relatório não registra desvios condicionais; por isso o total de branches é `0/0` e o JaCoCo o apresenta como 100%.

---
Desenvolvido como parte do Tech Challenge da FIAP.
