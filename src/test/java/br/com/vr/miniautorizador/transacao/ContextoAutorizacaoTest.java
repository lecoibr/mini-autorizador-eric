package br.com.vr.miniautorizador.transacao;

import static br.com.vr.miniautorizador.transacao.AfirmacoesAutorizacao.assertNaoAutorizadaPor;
import static org.assertj.core.api.Assertions.assertThat;

import br.com.vr.miniautorizador.cartao.Cartao;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ContextoAutorizacao")
class ContextoAutorizacaoTest {

    private static final String SENHA = "1234";

    @Test
    @DisplayName("Deve devolver o proprio cartao quando ele estiver presente")
    void deveDevolverCartaoQuandoPresente() {
        final Cartao cartao = Cartao.criar("6549873025634501", "hash", new BigDecimal("500.00"));
        final ContextoAutorizacao contexto = new ContextoAutorizacao(Optional.of(cartao), SENHA, BigDecimal.TEN);

        assertThat(contexto.cartaoExistente()).isSameAs(cartao);
    }

    @Test
    @DisplayName("Deve lancar TransacaoNaoAutorizadaException com CARTAO_INEXISTENTE quando o cartao estiver ausente")
    void deveLancarCartaoInexistenteQuandoAusente() {
        final ContextoAutorizacao contexto = new ContextoAutorizacao(Optional.empty(), SENHA, BigDecimal.TEN);

        assertNaoAutorizadaPor(contexto::cartaoExistente, MotivoNaoAutorizacao.CARTAO_INEXISTENTE);
    }
}
