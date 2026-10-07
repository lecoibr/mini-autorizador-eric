package br.com.vr.miniautorizador.integracao;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;

/**
 * Base dos testes de integração: sobe um único MySQL real (mesma versão do docker-compose do desafio) por JVM
 * e o compartilha entre todas as classes. O container é iniciado uma só vez porque o Spring reaproveita o
 * contexto entre classes de teste; reiniciar o container a cada classe deixaria o contexto em cache apontando
 * para uma porta que não existe mais. {@code @ServiceConnection} dispensa configurar URL, usuário e senha, e o
 * Flyway aplica a migration real ao iniciar a aplicação.
 */
public abstract class IntegracaoMysqlBase {

    @ServiceConnection
    protected static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:5.7");

    static {
        MYSQL.start();
    }
}
