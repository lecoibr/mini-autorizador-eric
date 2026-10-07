#!/usr/bin/env sh
# Sobe toda a aplicação (MySQL via Docker Compose + API Spring Boot) com um único comando.
# Pré-requisitos: Java 21+ e Docker em execução. Maven não é necessário (usa o Maven Wrapper).

set -e
cd "$(dirname "$0")"

if ! docker info >/dev/null 2>&1; then
    echo "[ERRO] Docker não está em execução. Inicie o Docker e tente novamente."
    exit 1
fi

echo "Iniciando o mini-autorizador em http://localhost:8080 ..."
echo "Documentação da API: http://localhost:8080/swagger-ui.html"
echo

chmod +x mvnw
./mvnw -q spring-boot:run
