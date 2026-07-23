FROM maven:3.9.9-eclipse-temurin-11 AS build

WORKDIR /workspace/backend
COPY backend/pom.xml ./pom.xml
RUN mvn -q -DskipTests dependency:go-offline
COPY backend/src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:11-jre-jammy

RUN apt-get update \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
        ca-certificates \
        curl \
        fonts-noto-cjk \
        gnupg \
        libreoffice-core \
        libreoffice-writer \
        poppler-utils \
        tesseract-ocr \
        tesseract-ocr-chi-sim \
    && install -d /usr/share/postgresql-common/pgdg \
    && curl -fsSL https://www.postgresql.org/media/keys/ACCC4CF8.asc \
        -o /usr/share/postgresql-common/pgdg/apt.postgresql.org.asc \
    && echo "deb [signed-by=/usr/share/postgresql-common/pgdg/apt.postgresql.org.asc] https://apt.postgresql.org/pub/repos/apt jammy-pgdg main" \
        > /etc/apt/sources.list.d/pgdg.list \
    && apt-get update \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends postgresql-client-16 \
    && pg_dump --version \
    && rm -rf /var/lib/apt/lists/*

RUN groupadd --gid 10001 zgai \
    && useradd --uid 10001 --gid 10001 --create-home --shell /usr/sbin/nologin zgai \
    && mkdir -p /app/data /app/logs /data/case-files /data/knowledge-files \
        /data/backups /data/knowledge-imports /data/document-intake \
    && chown -R zgai:zgai /app /data

WORKDIR /app
COPY --from=build --chown=zgai:zgai /workspace/backend/target/lawfirm-backend-2.0.0.jar /app/zgai.jar

USER zgai
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=5 \
    CMD curl -fsS http://127.0.0.1:8080/api/health || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-Djava.io.tmpdir=/tmp", "-jar", "/app/zgai.jar"]
