# ===== Estagio 1: build =====
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Cacheia as dependencias: baixa antes de copiar o codigo-fonte.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ===== Estagio 2: runtime =====
FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

# Usuario nao-root por seguranca.
RUN groupadd --system ledgerflow && useradd --system --gid ledgerflow ledgerflow

COPY --from=build /app/target/*.jar app.jar
RUN chown ledgerflow:ledgerflow app.jar

USER ledgerflow
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
