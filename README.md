# 16SOAT-TechChallenge1 - Oficina Mecânica - Sistema de Gestão de Ordens de Serviço (MVP)

Sistema Integrado de Atendimento e Execução de Serviços, que permitirá aos clientes acompanhar em tempo real o andamento do serviço, autorizar reparos adicionais via aplicativo e garantir uma gestão interna eficiente e segura.

Este projeto é o Back-end da primeira versão (MVP) do sistema de gestão para uma oficina mecânica de médio porte. O foco principal é a gestão eficiente de ordens de serviço, clientes, veículos e peças, garantindo qualidade e escalabilidade.

## 🛠 Stack Tecnológica

*   **Linguagem:** Java 21 (LTS)
*   **Framework:** Spring Boot 4.0.6
*   **Gerenciador de Dependências:** Maven
*   **Banco de Dados:** PostgreSQL
*   **Segurança:** Spring Security & OAuth2 Resource Server (Integração com Keycloak via JWT)
*   **Mapeamento de Objetos:** MapStruct 1.5.5.Final & Lombok
*   **Documentação da API:** Springdoc OpenAPI UI (Swagger) v3.0.3
*   **Qualidade & Testes:** JUnit 5, Testcontainers (PostgreSQL Real), Jacoco, SonarQube

## 📐 Arquitetura e Design

A aplicação foi desenvolvida seguindo os princípios da **Arquitetura em camadas**  combinado com **Domain-Driven Design (DDD)** para garantir um modelo de domínio rico e isolado.

*   **Modelos Ricos:** Lógica de negócio encapsulada nas entidades de domínio.
*   **Value Objects:** Uso de Java Records para imutabilidade e encapsulamento de atributos.

Diagramas de Domain Storytelling e Event Storming:
https://miro.com/welcomeonboard/UkU2NWZwU0tQSGplQVd2ckpBbHlHdnkxWThxcmh0K1RuTnB5NS83N0E1Z1FDMFhYK0dQUC9wbHpPM3F0NUhzdVBQWGZsMGNKckxIUzdiYTM0T3RhcmtzSWVKdTduSlRNUTY3aHBBNVN2RUhFMG80VDZwSXB6eFJEV1MxbnV2eVhzVXVvMm53MW9OWFg5bkJoVXZxdFhRPT0hdjE=?share_link_id=737223824198

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

### Passos para execução

1.  **Clonar o repositório:**
    ```bash
    git clone <repository-url>
    cd 16SOAT-TechChallenge1
    ```

2. **Compilar e Rodar a Aplicação:**
   ```bash
   docker-compose up  --build -d
   ```

## 🧪 Testes

Para executar apenas os testes:
```bash
./mvnw test
```

Para executar o ciclo completo com empacotamento e geração do relatório de cobertura via SonarQube (certifique-se de que o compose já esteja rodando):


1. **No seu navegador, acesse: http://localhost:9000 (ou http://127.0.0.1:9000)**

2. **Faça login utilizando as credenciais padrão:**
    - Usuário: admin
    - Senha: admin

3. **O sistema solicitará que você redefina a senha para um novo valor de sua preferência.**

4. **Na tela inicial do SonarQube, clique em Create Project e depois em Manually.**

5. **Preencha os campos:**
    - Project Key: 16SOAT-TechChallenge
    - Display Name: 16SOAT-TechChallenge

6. **Escolha a opção de analisar o código Localmente (Locally).**

7. **Na etapa seguinte, clique em Generate para criar um Token. Copie este token imediatamente, pois ele não será exibido novamente.**

8. **Execute o comando:**
    ```bash
    ./mvnw clean verify sonar:sonar "-Dsonar.token=<seu_token>"
    ```

9. **Aguarde o processo finalizar com a mensagem BUILD SUCCESS.**

10. **Retorne ao seu navegador na página do SonarQube (http://localhost:9000). A página do seu projeto será atualizada automaticamente.**

> Observação: o `verify` validado neste projeto utiliza a infraestrutura disponível no `docker-compose.yml` e um teste de contexto sobe a aplicação com o profile `dev`.


---
Desenvolvido como parte do Tech Challenge da FIAP.
