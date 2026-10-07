package br.com.vr.miniautorizador.integracao;

import java.math.BigDecimal;
import java.util.Map;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

/**
 * Cliente HTTP autenticado (Basic) da API, usado pelos testes de integração para falar com a aplicação
 * exatamente como o avaliador do desafio faria.
 */
final class ClienteApi {

    static final String USUARIO_API = "username";
    static final String SENHA_API = "password";

    private static final String URL_CARTOES = "/cartoes";
    private static final String URL_CARTAO = "/cartoes/{numeroCartao}";
    private static final String URL_TRANSACOES = "/transacoes";

    private final TestRestTemplate rest;

    ClienteApi(final TestRestTemplate base) {
        this.rest = base.withBasicAuth(USUARIO_API, SENHA_API);
    }

    ResponseEntity<String> criarCartao(final String numeroCartao, final String senha) {
        return rest.postForEntity(URL_CARTOES, Map.of("numeroCartao", numeroCartao, "senha", senha), String.class);
    }

    ResponseEntity<String> consultarSaldoBruto(final String numeroCartao) {
        return rest.getForEntity(URL_CARTAO, String.class, numeroCartao);
    }

    BigDecimal consultarSaldo(final String numeroCartao) {
        return rest.getForEntity(URL_CARTAO, BigDecimal.class, numeroCartao).getBody();
    }

    ResponseEntity<String> transacionar(final String numeroCartao, final String senhaCartao, final BigDecimal valor) {
        final Map<String, Object> corpo = Map.of(
                "numeroCartao", numeroCartao,
                "senhaCartao", senhaCartao,
                "valor", valor);
        return rest.postForEntity(URL_TRANSACOES, corpo, String.class);
    }
}
