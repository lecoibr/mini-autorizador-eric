package br.com.vr.miniautorizador;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Ponto de entrada do mini-autorizador: aplicação REST que cria cartões de benefício,
 * consulta saldo e autoriza (ou nega) transações aplicando as regras de autorização.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class MiniAutorizadorApplication {

    public static void main(final String[] args) {
        SpringApplication.run(MiniAutorizadorApplication.class, args);
    }
}
