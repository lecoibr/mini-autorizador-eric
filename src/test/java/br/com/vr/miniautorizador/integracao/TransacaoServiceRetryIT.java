package br.com.vr.miniautorizador.integracao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import br.com.vr.miniautorizador.cartao.Cartao;
import br.com.vr.miniautorizador.cartao.CartaoRepository;
import br.com.vr.miniautorizador.transacao.ConcorrenciaTransacaoException;
import br.com.vr.miniautorizador.transacao.MotivoNaoAutorizacao;
import br.com.vr.miniautorizador.transacao.TransacaoNaoAutorizadaException;
import br.com.vr.miniautorizador.transacao.TransacaoRequest;
import br.com.vr.miniautorizador.transacao.TransacaoService;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.AopTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

// Ambiente web MOCK (padrão): a configuração de segurança precisa do HttpSecurity, que não existe com WebEnvironment.NONE.
@SpringBootTest
@DisplayName("TransacaoService - retentativa por lock otimista (MySQL real)")
class TransacaoServiceRetryIT extends IntegracaoMysqlBase {

    private static final String NUMERO = "6549873025634501";
    private static final String SENHA = "1234";
    private static final BigDecimal SALDO_INICIAL = new BigDecimal("500.00");
    // Deve acompanhar MAXIMO_TENTATIVAS de TransacaoService.
    private static final int TENTATIVAS_MAXIMAS = 5;

    @MockitoSpyBean
    private CartaoRepository repository;

    @Autowired
    private TransacaoService service;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private final TransacaoRequest request = new TransacaoRequest(NUMERO, SENHA, new BigDecimal("100.00"));

    @BeforeEach
    void prepararCenario() {
        repository.deleteAll();
        repository.saveAndFlush(Cartao.criar(NUMERO, passwordEncoder.encode(SENHA), SALDO_INICIAL));
        clearInvocations(repository);
    }

    private ObjectOptimisticLockingFailureException conflitoDeVersao() {
        return new ObjectOptimisticLockingFailureException(Cartao.class, 1L);
    }

    /**
     * Faz o primeiro {@code saveAndFlush} falhar por lock otimista (executando antes a ação que simula a transação
     * concorrente vencedora) e delega as chamadas seguintes ao repositório real. O repositório é um proxy de
     * interface do Spring Data, então o spy do Spring não tem "método real" para o Mockito chamar; a delegação é
     * feita pelo default answer do próprio spy, que aponta para o bean original.
     */
    private void falharNoPrimeiroFlushDepoisDelegar(final Runnable acaoConcorrente) {
        final Answer<?> delegarAoRepositorioReal = mockingDetails(repository).getMockCreationSettings().getDefaultAnswer();
        final AtomicInteger chamadas = new AtomicInteger();
        doAnswer(invocacao -> {
            if (chamadas.incrementAndGet() == 1) {
                acaoConcorrente.run();
                throw conflitoDeVersao();
            }
            return delegarAoRepositorioReal.answer(invocacao);
        }).when(repository).saveAndFlush(any(Cartao.class));
    }

    /**
     * Simula a instância concorrente que venceu a corrida: em transação própria (REQUIRES_NEW), debita o saldo
     * do cartão até zerar, antes de a tentativa atual tentar gravar.
     */
    private void zerarSaldoEmTransacaoConcorrente() {
        final TransactionTemplate transacaoIndependente = new TransactionTemplate(transactionManager);
        transacaoIndependente.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transacaoIndependente.executeWithoutResult(status -> {
            final CartaoRepository repositorioReal = (CartaoRepository) AopTestUtils.getUltimateTargetObject(repository);
            final Cartao cartao = repositorioReal.findByNumeroCartao(NUMERO).orElseThrow();
            cartao.debitar(cartao.getSaldo());
            repositorioReal.saveAndFlush(cartao);
        });
    }

    @Test
    @DisplayName("Deve negar por SALDO_INSUFICIENTE na retentativa quando a transacao concorrente consumir o saldo")
    void deveNegarPorSaldoInsuficienteNaRetentativaQuandoConcorrenteConsumirSaldo() {
        falharNoPrimeiroFlushDepoisDelegar(this::zerarSaldoEmTransacaoConcorrente);

        assertThatThrownBy(() -> service.autorizar(request))
                .isInstanceOfSatisfying(TransacaoNaoAutorizadaException.class,
                        excecao -> assertThat(excecao.getMotivo()).isEqualTo(MotivoNaoAutorizacao.SALDO_INSUFICIENTE));

        final Cartao persistido = cartaoPersistido();
        assertThat(persistido.getSaldo()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    private Cartao cartaoPersistido() {
        return repository.findByNumeroCartao(NUMERO).orElseThrow();
    }

    @Test
    @DisplayName("Deve concluir com sucesso e debitar uma unica vez quando o primeiro flush falhar por lock otimista")
    void deveConcluirEDebitarUmaVezQuandoPrimeiroFlushFalhar() {
        falharNoPrimeiroFlushDepoisDelegar(() -> { });

        assertThatCode(() -> service.autorizar(request)).doesNotThrowAnyException();

        verify(repository, times(2)).saveAndFlush(any(Cartao.class));
        final Cartao persistido = cartaoPersistido();
        assertThat(persistido.getSaldo()).isEqualByComparingTo("400.00");
        assertThat(persistido.getVersao()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Deve lancar ConcorrenciaTransacaoException apos esgotar as tentativas quando o flush sempre falhar")
    void deveLancarConcorrenciaAposEsgotarTentativasQuandoFlushSempreFalhar() {
        doThrow(conflitoDeVersao()).when(repository).saveAndFlush(any(Cartao.class));

        assertThatThrownBy(() -> service.autorizar(request))
                .isInstanceOf(ConcorrenciaTransacaoException.class)
                .hasMessageContaining(NUMERO)
                .hasCauseInstanceOf(OptimisticLockingFailureException.class);

        verify(repository, times(TENTATIVAS_MAXIMAS)).saveAndFlush(any(Cartao.class));
        final Cartao persistido = cartaoPersistido();
        assertThat(persistido.getSaldo()).isEqualByComparingTo(SALDO_INICIAL);
        assertThat(persistido.getVersao()).isZero();
    }
}
