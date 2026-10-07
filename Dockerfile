# Etapa 1: build da aplicação com Maven e JDK 21
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /workspace

# Baixa as dependências antes de copiar o código-fonte para aproveitar o cache de camadas do Docker
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

COPY src ./src
RUN mvn -q -B package -DskipTests

# Etapa 2: imagem final enxuta, somente com o JRE e o jar, executando como usuário não privilegiado
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S miniautorizador && adduser -S -G miniautorizador miniautorizador
USER miniautorizador

COPY --from=build /workspace/target/mini-autorizador-*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
