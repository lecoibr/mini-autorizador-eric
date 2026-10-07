package br.com.vr.miniautorizador.transacao;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AutorizadorTransacao")
class AutorizadorTransacaoTest {

    private static final ContextoAutorizacao CONTEXTO =
            new ContextoAutorizacao(Optional.empty(), "1234", BigDecimal.TEN);

    @Mock
    private RegraAutorizacao primeira;

    @Mock
    private RegraAutorizacao segunda;

    @Mock
    private RegraAutorizacao terceira;

    private List<RegraAutorizacao> regras;

    @BeforeEach
    void configurar() {
        regras = new ArrayList<>(List.of(primeira, segunda, terceira));
    }

    @Test
    @DisplayName("Deve executar todas as regras na ordem da lista quando nenhuma barrar")
    void deveExecutarTodasAsRegrasNaOrdemQuandoNenhumaBarrar() {
        final AutorizadorTransacao autorizador = new AutorizadorTransacao(regras);

        autorizador.autorizar(CONTEXTO);

        final InOrder ordem = inOrder(primeira, segunda, terceira);
        ordem.verify(primeira).validar(CONTEXTO);
        ordem.verify(segunda).validar(CONTEXTO);
        ordem.verify(terceira).validar(CONTEXTO);
        verifyNoMoreInteractions(primeira, segunda, terceira);
    }

    @Test
    @DisplayName("Deve interromper e propagar a excecao quando uma regra barrar, sem executar as seguintes")
    void deveInterromperQuandoUmaRegraBarrar() {
        final TransacaoNaoAutorizadaException barramento =
                new TransacaoNaoAutorizadaException(MotivoNaoAutorizacao.SENHA_INVALIDA);
        doThrow(barramento).when(segunda).validar(CONTEXTO);
        final AutorizadorTransacao autorizador = new AutorizadorTransacao(regras);

        assertThatThrownBy(() -> autorizador.autorizar(CONTEXTO)).isSameAs(barramento);

        verify(primeira).validar(CONTEXTO);
        verify(segunda).validar(CONTEXTO);
        verify(terceira, never()).validar(CONTEXTO);
    }

    @Test
    @DisplayName("Deve nao lancar excecao quando nao houver regras")
    void deveNaoLancarExcecaoQuandoNaoHouverRegras() {
        final AutorizadorTransacao autorizador = new AutorizadorTransacao(List.of());

        assertThatCode(() -> autorizador.autorizar(CONTEXTO)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Deve ignorar regras adicionadas na lista original depois da construcao")
    void deveIgnorarRegrasAdicionadasDepoisDaConstrucao() {
        final AutorizadorTransacao autorizador = new AutorizadorTransacao(regras);
        final RegraAutorizacao tardia = mock(RegraAutorizacao.class);
        regras.add(tardia);

        autorizador.autorizar(CONTEXTO);

        verify(tardia, never()).validar(CONTEXTO);
        verify(terceira).validar(CONTEXTO);
    }
}
