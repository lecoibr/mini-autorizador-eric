package br.com.vr.miniautorizador.integracao;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.vr.miniautorizador.cartao.Cartao;
import br.com.vr.miniautorizador.cartao.CartaoRepository;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Fluxo do avaliador (MySQL real)")
class FluxoAvaliadorIT extends IntegracaoMysqlBase {

    private static final String NUMERO = "6549873025634501";
    private static final String SENHA = "1234";
    private static final String SENHA_ERRADA = "0000";
    private static final String URL_CARTAO = "/cartoes/" + NUMERO;
    private static final String JSON_ECO = "{\"senha\":\"1234\",\"numeroCartao\":\"6549873025634501\"}";
    private static final BigDecimal SALDO_INICIAL = new BigDecimal("500.00");
    private static final BigDecimal VALOR_TRANSACAO = new BigDecimal("100.00");
    private static final int LIMITE_DE_SEGURANCA = 50;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CartaoRepository repository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private ClienteApi api;

    @BeforeEach
    void limparBanco() {
        repository.deleteAll();
        api = new ClienteApi(restTemplate);
    }

    private Cartao cartaoPersistido() {
        return repository.findByNumeroCartao(NUMERO).orElseThrow();
    }

    @Test
    @DisplayName("Deve cumprir o roteiro do README na ordem quando o avaliador exercitar a API")
    void deveCumprirRoteiroDoReadmeQuandoAvaliadorExercitarApi() {
        // 1. criacao do cartao
        final ResponseEntity<String> criacao = api.criarCartao(NUMERO, SENHA);
        assertThat(criacao.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(criacao.getBody()).isEqualTo(JSON_ECO);
        assertThat(cartaoPersistido().getVersao()).isZero();

        // 2. saldo do cartao recem-criado
        final ResponseEntity<String> consultaInicial = api.consultarSaldoBruto(NUMERO);
        assertThat(consultaInicial.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(new BigDecimal(consultaInicial.getBody())).isEqualByComparingTo(SALDO_INICIAL);

        // 3. transacoes ate o saldo acabar, conferindo o saldo apos cada uma
        int aprovadas = 0;
        ResponseEntity<String> resposta = api.transacionar(NUMERO, SENHA, VALOR_TRANSACAO);
        while (resposta.getStatusCode().is2xxSuccessful() && aprovadas < LIMITE_DE_SEGURANCA) {
            aprovadas++;
            assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(resposta.getBody()).isEqualTo("OK");
            final BigDecimal esperado = SALDO_INICIAL.subtract(VALOR_TRANSACAO.multiply(BigDecimal.valueOf(aprovadas)));
            assertThat(api.consultarSaldo(NUMERO)).isEqualByComparingTo(esperado);
            resposta = api.transacionar(NUMERO, SENHA, VALOR_TRANSACAO);
        }
        assertThat(aprovadas).isEqualTo(5);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(resposta.getBody()).isEqualTo("SALDO_INSUFICIENTE");
        assertThat(api.consultarSaldo(NUMERO)).isEqualByComparingTo(BigDecimal.ZERO);

        // 4. senha invalida nao altera o saldo
        final ResponseEntity<String> senhaInvalida = api.transacionar(NUMERO, SENHA_ERRADA, BigDecimal.ONE);
        assertThat(senhaInvalida.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(senhaInvalida.getBody()).isEqualTo("SENHA_INVALIDA");
        assertThat(api.consultarSaldo(NUMERO)).isEqualByComparingTo(BigDecimal.ZERO);

        // 5. cartao inexistente
        final ResponseEntity<String> inexistente = api.transacionar("9999999999999999", SENHA, BigDecimal.ONE);
        assertThat(inexistente.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(inexistente.getBody()).isEqualTo("CARTAO_INEXISTENTE");
    }

    @Test
    @DisplayName("Deve persistir a senha como hash e incrementar a versao a cada debito aprovado")
    void devePersistirHashEIncrementarVersaoQuandoDebitosAprovados() {
        api.criarCartao(NUMERO, SENHA);
        final Long versaoInicial = cartaoPersistido().getVersao();

        api.transacionar(NUMERO, SENHA, VALOR_TRANSACAO);
        api.transacionar(NUMERO, SENHA, VALOR_TRANSACAO);
        api.transacionar(NUMERO, SENHA_ERRADA, VALOR_TRANSACAO);

        final Cartao persistido = cartaoPersistido();
        assertThat(persistido.getSenha()).isNotEqualTo(SENHA);
        assertThat(passwordEncoder.matches(SENHA, persistido.getSenha())).isTrue();
        assertThat(persistido.getSaldo()).isEqualByComparingTo("300.00");
        assertThat(persistido.getVersao()).isEqualTo(versaoInicial + 2);
    }

    @Test
    @DisplayName("Deve responder 422 com o mesmo corpo e preservar o cartao original quando o numero ja existir")
    void deveResponder422QuandoCartaoDuplicado() {
        api.criarCartao(NUMERO, SENHA);
        api.transacionar(NUMERO, SENHA, VALOR_TRANSACAO);

        final ResponseEntity<String> duplicado = api.criarCartao(NUMERO, SENHA);

        assertThat(duplicado.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(duplicado.getBody()).isEqualTo(JSON_ECO);
        assertThat(repository.count()).isEqualTo(1);
        assertThat(cartaoPersistido().getSaldo()).isEqualByComparingTo("400.00");
    }

    @Test
    @DisplayName("Deve responder 404 sem corpo quando consultar saldo de cartao inexistente")
    void deveResponder404QuandoCartaoInexistente() {
        final ResponseEntity<String> resposta = api.consultarSaldoBruto("0000000000000000");

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resposta.getBody()).isNullOrEmpty();
    }

    @Test
    @DisplayName("Deve responder 401 em todas as rotas e nada persistir quando nao houver credenciais")
    void deveResponder401QuandoSemCredenciais() {
        final ResponseEntity<String> criar = restTemplate.postForEntity("/cartoes",
                Map.of("numeroCartao", NUMERO, "senha", SENHA), String.class);
        final ResponseEntity<String> consultar = restTemplate.getForEntity(URL_CARTAO, String.class);
        final ResponseEntity<String> transacionar = restTemplate.postForEntity("/transacoes",
                Map.of("numeroCartao", NUMERO, "senhaCartao", SENHA, "valor", BigDecimal.ONE), String.class);

        assertThat(criar.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(consultar.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(transacionar.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(repository.count()).isZero();
    }

    @Test
    @DisplayName("Deve responder 401 quando o usuario da API existir mas a senha estiver errada")
    void deveResponder401QuandoSenhaDaApiErrada() {
        api.criarCartao(NUMERO, SENHA);

        final ResponseEntity<String> resposta = restTemplate.withBasicAuth(ClienteApi.USUARIO_API, "errada")
                .getForEntity(URL_CARTAO, String.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
