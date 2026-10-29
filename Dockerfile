# syntax=docker/dockerfile:1

# =============================================================================
# STAGE 1 — build
# O pom é copiado sozinho para que a resolução de dependências vire uma camada
# própria: alterar código-fonte não obriga o Maven a baixar tudo de novo.
# =============================================================================
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Agente do New Relic, na mesma versão da API declarada no pom (newrelic.version).
# Vem do Maven Central pelo próprio Maven, que confere o checksum do artefato.
# Camada logo após o pom: só é refeita quando a versão muda.
RUN mvn -B -q dependency:copy \
      -Dartifact=com.newrelic.agent.java:newrelic-agent:$(mvn -B -q help:evaluate -Dexpression=newrelic.version -DforceStdout) \
      -DoutputDirectory=/newrelic -Dmdep.stripVersion=true

COPY src ./src
RUN mvn -B -q clean package -DskipTests


# =============================================================================
# STAGE 2 — explode o JAR em camadas
# `-Djarmode=tools extract --layers` separa dependências (raramente mudam) do
# código da aplicação (muda a cada commit). Sem isso, um `docker push` reenvia
# ~60 MB de bibliotecas a cada build.
# =============================================================================
FROM eclipse-temurin:21-jre AS layers

WORKDIR /layers
COPY --from=build /build/target/*.jar app.jar
# O destino precisa ser um diretório vazio — daí `extracted/` em vez de `.`,
# que já contém o próprio app.jar.
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted


# =============================================================================
# STAGE 3 — runtime
# =============================================================================
FROM eclipse-temurin:21-jre

LABEL org.opencontainers.image.title="autopecas-api" \
      org.opencontainers.image.description="API de gestão de ordens de serviço de oficina mecânica" \
      org.opencontainers.image.source="https://github.com/FariasPNt/16SOAT-TechChallenge1"

# curl serve ao healthcheck do docker-compose. No Kubernetes as probes são
# feitas pelo kubelet via HTTP e não dependem de binário dentro da imagem.
RUN apt-get update \
    && apt-get install --no-install-recommends -y curl \
    && rm -rf /var/lib/apt/lists/*

# Usuário não-root: um comprometimento da aplicação não vira root no container
RUN useradd --create-home --shell /usr/sbin/nologin appuser

WORKDIR /app

# Ordem das camadas: da que menos muda para a que mais muda
COPY --from=layers --chown=appuser:appuser /layers/extracted/dependencies/ ./
COPY --from=layers --chown=appuser:appuser /layers/extracted/spring-boot-loader/ ./
COPY --from=layers --chown=appuser:appuser /layers/extracted/snapshot-dependencies/ ./
COPY --from=layers --chown=appuser:appuser /layers/extracted/application/ ./

# O agente procura o newrelic.yml no diretório do próprio jar.
COPY --from=build /newrelic/newrelic-agent.jar /app/newrelic/newrelic.jar
COPY infra/docker/newrelic.yml /app/newrelic/newrelic.yml

USER appuser

EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS=""

# -XX:MaxRAMPercentage respeita o limite de memória do container (cgroup), que é
#   o que o Kubernetes define em resources.limits.memory.
# -XX:+ExitOnOutOfMemoryError faz o processo morrer no OOM em vez de ficar de pé
#   e degradado: o orquestrador reinicia o pod, que é o comportamento desejado.
# -Duser.timezone=UTC alinha o @CreationTimestamp do Hibernate (que usa o fuso da
#   JVM) com o Relogio da aplicação e com a serialização Jackson.
# -javaagent só entra quando NEW_RELIC_LICENSE_KEY existe: no Compose e em dev a
#   imagem sobe sem o agente, em vez de subir com ele reclamando da key ausente.
ENTRYPOINT ["sh", "-c", "exec java \
  ${NEW_RELIC_LICENSE_KEY:+-javaagent:/app/newrelic/newrelic.jar} \
  -XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=75.0 \
  -XX:+ExitOnOutOfMemoryError \
  -Duser.timezone=UTC \
  -Djava.security.egd=file:/dev/./urandom \
  $JAVA_OPTS \
  org.springframework.boot.loader.launch.JarLauncher"]
