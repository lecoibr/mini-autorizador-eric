package br.com.vr.miniautorizador.cartao;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CartaoNaoEncontradoException")
class CartaoNaoEncontradoExceptionTest {

    @Test
    @DisplayName("Deve conter o numero do cartao na mensagem quando criada")
    void deveConterNumeroNaMensagemQuandoCriada() {
        final CartaoNaoEncontradoException excecao = new CartaoNaoEncontradoException("6549873025634501");

        assertThat(excecao.getMessage()).contains("6549873025634501");
        assertThat(excecao.getCause()).isNull();
    }
}
