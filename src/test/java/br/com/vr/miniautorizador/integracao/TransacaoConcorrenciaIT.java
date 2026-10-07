package br.com.vr.miniautorizador.integracao;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.vr.miniautorizador.cartao.CartaoRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Concorrencia de transacoes (MySQL real)")
class TransacaoConcorrenciaIT extends IntegracaoMysqlBase {

    private static final String NUMERO = "6549873025634501";
    private static final String SENHA = "1234";
    private static final String CORPO_CONFLITO = "CONFLITO_CONCORRENCIA";
    private static final String CORPO_SALDO_INSUFICIENTE = "SALDO_INSUFICIENTE";
    private static final BigDecimal VALOR = new BigDecimal("10.00");
    private static final long TIMEOUT_SEGUNDOS = 60;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CartaoRepository repository;

    private ClienteApi api;

    @BeforeEach
    void prepararCenario() {
        repository.deleteAll();
        api = new ClienteApi(restTemplate);
        assertThat(api.criarCartao(NUMERO, SENHA).getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    @DisplayName("Deve aprovar exatamente uma transacao e zerar o saldo quando 10 debitos simultaneos disputarem 10.00")
    void deveAprovarSomenteUmaQuandoDezDebitosDisputaremSaldoDeDezReais() throws Exception {
        assertThat(api.transacionar(NUMERO, SENHA, new BigDecimal("490.00")).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
        assertThat(api.consultarSaldo(NUMERO)).isEqualByComparingTo(VALOR);
        final Long versaoAntes = repository.findByNumeroCartao(NUMERO).orElseThrow().getVersao();

        final List<ResponseEntity<String>> respostas = dispararSimultaneas(10,
                () -> api.transacionar(NUMERO, SENHA, VALOR));

        assertThat(respostas).hasSize(10);
        assertThat(statusDe(respostas)).isNotEmpty().noneMatch(status -> status >= HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(respostas).filteredOn(this::foiAprovada).hasSize(1)
                .allSatisfy(resposta -> assertThat(resposta.getBody()).isEqualTo("OK"));
        // 409 e aceitavel apenas sob contencao extrema (tentativas esgotadas); o normal e 422 na retentativa.
        assertThat(respostas).filteredOn(resposta -> !foiAprovada(resposta)).hasSize(9)
                .allSatisfy(resposta -> assertThat(resposta.getBody())
                        .isIn(CORPO_SALDO_INSUFICIENTE, CORPO_CONFLITO));
        assertThat(api.consultarSaldo(NUMERO)).isEqualByComparingTo("0.00");
        assertThat(repository.findByNumeroCartao(NUMERO).orElseThrow().getVersao()).isEqualTo(versaoAntes + 1);
    }

    @Test
    @DisplayName("Deve manter o saldo coerente com as aprovacoes e sem erro 5xx quando 20 debitos disputarem o mesmo cartao")
    void deveManterSaldoCoerenteQuandoVinteDebitosDisputaremMesmoCartao() throws Exception {
        final int quantidade = 20;
        final Long versaoAntes = repository.findByNumeroCartao(NUMERO).orElseThrow().getVersao();

        final List<ResponseEntity<String>> respostas = dispararSimultaneas(quantidade,
                () -> api.transacionar(NUMERO, SENHA, VALOR));

        final long aprovadas = respostas.stream().filter(this::foiAprovada).count();
        final long conflitos = respostas.stream()
                .filter(resposta -> resposta.getStatusCode() == HttpStatus.CONFLICT).count();
        assertThat(respostas).hasSize(quantidade);
        assertThat(statusDe(respostas)).isNotEmpty().noneMatch(status -> status >= HttpStatus.INTERNAL_SERVER_ERROR.value());
        // Saldo suficiente para todas: so existem duas saidas possiveis, aprovada (201) ou conflito (409).
        // O 409 e aceitavel (lock otimista com tentativas esgotadas); o inaceitavel seria perder um debito.
        assertThat(aprovadas + conflitos).isEqualTo(quantidade);
        assertThat(aprovadas).isPositive();
        assertThat(respostas).filteredOn(resposta -> resposta.getStatusCode() == HttpStatus.CONFLICT)
                .allSatisfy(resposta -> assertThat(resposta.getBody()).isEqualTo(CORPO_CONFLITO));
        final BigDecimal saldoEsperado = new BigDecimal("500.00").subtract(VALOR.multiply(BigDecimal.valueOf(aprovadas)));
        assertThat(api.consultarSaldo(NUMERO)).isEqualByComparingTo(saldoEsperado);
        assertThat(repository.findByNumeroCartao(NUMERO).orElseThrow().getVersao()).isEqualTo(versaoAntes + aprovadas);
    }

    private boolean foiAprovada(final ResponseEntity<String> resposta) {
        return resposta.getStatusCode() == HttpStatus.CREATED;
    }

    private List<Integer> statusDe(final List<ResponseEntity<String>> respostas) {
        return respostas.stream().map(resposta -> resposta.getStatusCode().value()).toList();
    }

    /**
     * Dispara a mesma chamada em {@code quantidade} threads que ficam bloqueadas ate que todas estejam prontas,
     * para que as requisicoes cheguem juntas ao servidor.
     */
    private List<ResponseEntity<String>> dispararSimultaneas(final int quantidade,
                                                             final Callable<ResponseEntity<String>> chamada)
            throws Exception {
        final ExecutorService pool = Executors.newFixedThreadPool(quantidade);
        final CountDownLatch largada = new CountDownLatch(1);
        try {
            final CountDownLatch prontas = new CountDownLatch(quantidade);
            final List<Future<ResponseEntity<String>>> futuros = new ArrayList<>();
            for (int i = 0; i < quantidade; i++) {
                futuros.add(pool.submit(() -> {
                    prontas.countDown();
                    largada.await();
                    return chamada.call();
                }));
            }
            assertThat(prontas.await(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS)).isTrue();
            largada.countDown();

            final List<ResponseEntity<String>> respostas = new ArrayList<>();
            for (final Future<ResponseEntity<String>> futuro : futuros) {
                respostas.add(futuro.get(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS));
            }
            return respostas;
        } finally {
            // Libera a largada mesmo em falha, para nenhuma thread ficar presa no latch, e interrompe o restante.
            largada.countDown();
            pool.shutdownNow();
            assertThat(pool.awaitTermination(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS)).isTrue();
        }
    }
}
