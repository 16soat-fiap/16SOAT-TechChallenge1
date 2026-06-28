# =========================
# BUILD STAGE
# =========================
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B


# =========================
# RUNTIME STAGE
# =========================
FROM eclipse-temurin:21-jre

WORKDIR /app

# Cria usuário não-root (segurança)
RUN useradd -m appuser

# Copia o JAR definindo o dono imediatamente (evita criar camada extra de chown)
COPY --from=build --chown=appuser:appuser /app/target/*.jar app.jar

USER appuser

EXPOSE 8080

# JVM otimizada para container (sintaxe corrigida para uma única linha)
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]