package br.com.vr.miniautorizador.transacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.vr.miniautorizador.cartao.Cartao;
import br.com.vr.miniautorizador.cartao.CartaoRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransacaoService")
class TransacaoServiceTest {

    private static final String NUMERO = "6549873025634501";
    private static final String SENHA = "1234";
    private static final BigDecimal VALOR = new BigDecimal("100.00");
    private static final BigDecimal SALDO_INICIAL = new BigDecimal("500.00");

    @Mock
    private CartaoRepository repository;

    @Mock
    private AutorizadorTransacao autorizador;

    private TransacaoService service;
    private Cartao cartao;
    private TransacaoRequest request;

    @BeforeEach
    void configurar() {
        service = new TransacaoService(repository, autorizador);
        cartao = Cartao.criar(NUMERO, "hash", SALDO_INICIAL);
        request = new TransacaoRequest(NUMERO, SENHA, VALOR);
    }

    @Nested
    @DisplayName("autorizar")
    class Autorizar {

        @Test
        @DisplayName("Deve debitar o saldo e gravar o cartao quando o autorizador aprovar")
        void deveDebitarEGravarQuandoAutorizadorAprovar() {
            when(repository.findByNumeroCartao(NUMERO)).thenReturn(Optional.of(cartao));

            service.autorizar(request);

            final ArgumentCaptor<ContextoAutorizacao> captor = ArgumentCaptor.forClass(ContextoAutorizacao.class);
            verify(autorizador).autorizar(captor.capture());
            final ContextoAutorizacao contexto = captor.getValue();
            assertThat(contexto.cartao()).containsSame(cartao);
            assertThat(contexto.senhaInformada()).isEqualTo(SENHA);
            assertThat(contexto.valor()).isEqualTo(VALOR);
            assertThat(cartao.getSaldo()).isEqualTo(new BigDecimal("400.00"));
            verify(repository).saveAndFlush(cartao);
        }

        @Test
        @DisplayName("Deve propagar a excecao e nao gravar nem debitar quando o autorizador barrar")
        void devePropagarENaoGravarQuandoAutorizadorBarrar() {
            final TransacaoNaoAutorizadaException barramento =
                    new TransacaoNaoAutorizadaException(MotivoNaoAutorizacao.SALDO_INSUFICIENTE);
            when(repository.findByNumeroCartao(NUMERO)).thenReturn(Optional.of(cartao));
            doThrow(barramento).when(autorizador).autorizar(any(ContextoAutorizacao.class));

            assertThatThrownBy(() -> service.autorizar(request)).isSameAs(barramento);

            verify(repository, never()).saveAndFlush(any(Cartao.class));
            assertThat(cartao.getSaldo()).isEqualTo(SALDO_INICIAL);
        }

        @Test
        @DisplayName("Deve repassar contexto com cartao vazio ao autorizador quando o cartao nao existir")
        void deveRepassarContextoVazioQuandoCartaoNaoExistir() {
            final TransacaoNaoAutorizadaException barramento =
                    new TransacaoNaoAutorizadaException(MotivoNaoAutorizacao.CARTAO_INEXISTENTE);
            when(repository.findByNumeroCartao(NUMERO)).thenReturn(Optional.empty());
            doThrow(barramento).when(autorizador).autorizar(any(ContextoAutorizacao.class));

            assertThatThrownBy(() -> service.autorizar(request)).isSameAs(barramento);

            final ArgumentCaptor<ContextoAutorizacao> captor = ArgumentCaptor.forClass(ContextoAutorizacao.class);
            verify(autorizador).autorizar(captor.capture());
            assertThat(captor.getValue().cartao()).isEmpty();
            verify(repository, never()).saveAndFlush(any(Cartao.class));
        }
    }

    @Nested
    @DisplayName("recuperar")
    class Recuperar {

        @Test
        @DisplayName("Deve lancar ConcorrenciaTransacaoException com a causa e o numero do cartao quando recuperar")
        void deveLancarConcorrenciaComCausaQuandoRecuperar() {
            final OptimisticLockingFailureException causa = new OptimisticLockingFailureException("versao desatualizada");

            assertThatThrownBy(() -> service.recuperar(causa, request))
                    .isInstanceOf(ConcorrenciaTransacaoException.class)
                    .hasMessageContaining(NUMERO)
                    .hasCause(causa);
        }

        @Test
        @DisplayName("Deve propagar a mesma excecao, sem envolver, quando a falha nao for de concorrencia")
        void devePropagarMesmaExcecaoQuandoFalhaNaoForDeConcorrencia() {
            final TransacaoNaoAutorizadaException causa =
                    new TransacaoNaoAutorizadaException(MotivoNaoAutorizacao.SALDO_INSUFICIENTE);

            assertThatThrownBy(() -> service.propagarFalhaNaoRetentavel(causa, request))
                    .isSameAs(causa);
        }
    }
}
