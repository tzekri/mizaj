# Image unique : le backend Spring Boot sert aussi le site React (même domaine, pas de CORS).
# Utilisée par Render (voir render.yaml).

# --- 1. Frontend (à la racine du dépôt) ---
FROM node:20-alpine AS frontend
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY index.html tsconfig.json vite.config.ts ./
COPY public ./public
COPY src ./src
RUN npm run build

# --- 2. Backend (embarque le build du frontend dans /static) ---
FROM eclipse-temurin:21-jdk AS backend
WORKDIR /app
COPY backend/.mvn/ .mvn/
COPY backend/mvnw backend/pom.xml ./
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY backend/src ./src
COPY --from=frontend /app/dist ./src/main/resources/static
RUN ./mvnw -B -q clean package -DskipTests

# --- 3. Exécution ---
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=backend /app/target/*.jar app.jar
# Render fournit la variable PORT ; 8095 par défaut ailleurs.
EXPOSE 8095
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
