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
    cd TechChallenge
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

Para executar os testes unitários e de integração:
```bash
./mvnw test
```

---
Desenvolvido como parte do Tech Challenge da FIAP.
