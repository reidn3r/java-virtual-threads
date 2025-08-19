# ---- BUILD STAGE ----
FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app

# Copia apenas os arquivos de dependência primeiro (para cachear dependências)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copia o resto do código e compila
COPY src ./src
RUN mvn clean package -DskipTests

# ---- RUNTIME STAGE ----
FROM eclipse-temurin:21-jre-alpine AS runtime
# outra opção ainda mais leve: gcr.io/distroless/java21-debian12

WORKDIR /app

# Copia o jar do estágio de build
COPY --from=build /app/target/*.jar app.jar

# expõe a porta do Spring Boot
EXPOSE 8080

# Entrypoint da aplicação
ENTRYPOINT ["java","-jar","app.jar"]
