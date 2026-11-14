# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Maven Wrapper oder System-Maven nutzen
RUN apk add --no-cache maven

# Abhängigkeiten zuerst cachen (Layer-Optimierung)
COPY pom.xml ./
RUN mvn dependency:go-offline -q

# Quellcode kopieren und bauen
COPY src ./src
RUN mvn package -DskipTests -q

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Wget für den Healthcheck
RUN apk add --no-cache wget

# Nur das fertige JAR kopieren
COPY --from=builder /app/target/space-invaders-*.jar app.jar

# Datenbankverzeichnis anlegen (wird als Volume gemountet)
RUN mkdir -p /app/data

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=20s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080 || exit 1

# Headless-Modus: AWT-Klassen ohne Display nutzbar
ENV JAVA_OPTS="-Djava.awt.headless=true -XX:+UseContainerSupport -XX:MaxRAMPercentage=75"

CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
