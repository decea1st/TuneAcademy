# One image for the whole app: React is built first and packaged inside the Spring Boot jar,
# which then serves both the pages and /api on port 8080.

# 1. Build the React app.
FROM node:22-alpine AS web
WORKDIR /web
COPY package.json package-lock.json ./
RUN npm ci
COPY index.html vite.config.ts tsconfig.json ./
COPY public public
COPY src src
RUN npm run build

# 2. Build the Spring Boot jar, with the React build as its static files.
FROM eclipse-temurin:21-jdk AS api
WORKDIR /api
COPY backend/mvnw backend/pom.xml ./
COPY backend/.mvn .mvn
# Downloads the dependencies in their own layer, so code-only changes rebuild faster.
RUN ./mvnw -B -q dependency:go-offline
COPY backend/src src
COPY --from=web /web/dist src/main/resources/static
# The tests run in CI, against a real database, before this image is built.
RUN ./mvnw -B -q -DskipTests package

# 3. Run it: a Java runtime and the jar, as a non-root user.
FROM eclipse-temurin:21-jre
RUN useradd --system --uid 1001 app
WORKDIR /app
COPY --from=api /api/target/*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
