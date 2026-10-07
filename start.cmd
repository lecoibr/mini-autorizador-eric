@echo off
setlocal

rem Sobe toda a aplicacao (MySQL via Docker Compose + API Spring Boot) com um unico comando.
rem Pre-requisitos: Java 21+ e Docker em execucao. Maven nao e necessario (usa o Maven Wrapper).

cd /d "%~dp0"

docker info >nul 2>&1
if errorlevel 1 (
    echo [ERRO] Docker nao esta em execucao. Inicie o Docker Desktop e tente novamente.
    exit /b 1
)

echo Iniciando o mini-autorizador em http://localhost:8080 ...
echo Documentacao da API: http://localhost:8080/swagger-ui.html
echo.

call mvnw.cmd -q spring-boot:run

endlocal
