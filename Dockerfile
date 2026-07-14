# ===== Estagio 1: build =====
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Cacheia as dependencias: baixa antes de copiar o codigo-fonte.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ===== Estagio 2: runtime =====
# Ubuntu 24.04 (noble) traz o Tesseract 5, cujo tessdata fica em
# /usr/share/tesseract-ocr/5/tessdata (bate com OCR_TESSDATA_PATH padrao).
FROM eclipse-temurin:21-jre-noble AS runtime
WORKDIR /app

# OCR: necessario para ler PDFs escaneados e imagens (PDF com texto nativo
# e lido pelo PDFBox, sem depender disto). "por" = portugues.
RUN apt-get update \
    && apt-get install -y --no-install-recommends tesseract-ocr tesseract-ocr-por \
    && rm -rf /var/lib/apt/lists/*

# Caminho do tessdata e idioma padrao do OCR.
ENV OCR_TESSDATA_PATH=/usr/share/tesseract-ocr/5/tessdata \
    OCR_LANGUAGE=por

# Usuario nao-root por seguranca.
RUN groupadd --system nalitech && useradd --system --gid nalitech nalitech

COPY --from=build /app/target/*.jar app.jar
RUN chown nalitech:nalitech app.jar

USER nalitech
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
