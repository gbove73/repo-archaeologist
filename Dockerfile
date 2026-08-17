FROM eclipse-temurin:25-jdk-noble AS builder

WORKDIR /workspace
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw --batch-mode dependency:go-offline

COPY src/ src/
RUN ./mvnw --batch-mode package -DskipTests

FROM eclipse-temurin:25-jre-noble

RUN apt-get update \
    && apt-get install --yes --no-install-recommends ca-certificates git \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --create-home --uid 10001 archaeologist

WORKDIR /app
COPY --from=builder /workspace/target/repo-archaeologist-*.jar app.jar

USER archaeologist
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
