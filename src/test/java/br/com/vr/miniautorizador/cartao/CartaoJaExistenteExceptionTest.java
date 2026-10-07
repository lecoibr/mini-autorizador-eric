package br.com.vr.miniautorizador.cartao;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CartaoJaExistenteException")
class CartaoJaExistenteExceptionTest {

    private static final CartaoRequest REQUEST = new CartaoRequest("6549873025634501", "1234");

    @Test
    @DisplayName("Deve conter o numero do cartao na mensagem e nao ter causa quando criada sem causa")
    void deveConterNumeroNaMensagemQuandoCriadaSemCausa() {
        final CartaoJaExistenteException excecao = new CartaoJaExistenteException(REQUEST);

        assertThat(excecao.getMessage()).contains(REQUEST.numeroCartao());
        assertThat(excecao.getCause()).isNull();
        assertThat(excecao.getRequest()).isSameAs(REQUEST);
    }

    @Test
    @DisplayName("Deve preservar a causa quando criada com causa")
    void devePreservarCausaQuandoCriadaComCausa() {
        final IllegalStateException causa = new IllegalStateException("violacao");

        final CartaoJaExistenteException excecao = new CartaoJaExistenteException(REQUEST, causa);

        assertThat(excecao.getCause()).isSameAs(causa);
        assertThat(excecao.getMessage()).contains(REQUEST.numeroCartao());
        assertThat(excecao.getRequest()).isSameAs(REQUEST);
    }
}
