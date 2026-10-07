package br.com.vr.miniautorizador.integracao;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.vr.miniautorizador.cartao.CartaoProperties;
import br.com.vr.miniautorizador.transacao.RegraAutorizacao;
import br.com.vr.miniautorizador.transacao.RegraCartaoExistente;
import br.com.vr.miniautorizador.transacao.RegraSaldoSuficiente;
import br.com.vr.miniautorizador.transacao.RegraSenhaValida;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("MiniAutorizadorApplication (MySQL real)")
class MiniAutorizadorApplicationIT extends IntegracaoMysqlBase {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private List<RegraAutorizacao> regras;

    @Autowired
    private CartaoProperties cartaoProperties;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Deve carregar o contexto com as regras na ordem de autorizacao e saldo inicial de 500.00")
    void deveCarregarContextoQuandoAplicacaoSubir() {
        assertThat(regras).hasExactlyElementsOfTypes(
                RegraCartaoExistente.class, RegraSenhaValida.class, RegraSaldoSuficiente.class);
        assertThat(cartaoProperties.saldoInicial()).isEqualByComparingTo(new BigDecimal("500.00"));
    }

    @Test
    @DisplayName("Deve ter aplicado a migration do Flyway criando a tabela cartao")
    void deveTerAplicadoMigrationQuandoContextoSubir() {
        final Integer tabelas = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables "
                        + "WHERE table_schema = DATABASE() AND table_name = 'cartao'", Integer.class);

        assertThat(tabelas).isEqualTo(1);
    }

    @Test
    @DisplayName("Deve responder 200 com status UP no health check sem autenticacao")
    void deveResponderHealthUpQuandoSemAutenticacao() {
        final ResponseEntity<String> resposta = restTemplate.getForEntity("/actuator/health", String.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).contains("\"status\":\"UP\"");
    }
}
