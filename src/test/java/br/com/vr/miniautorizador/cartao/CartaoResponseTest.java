package br.com.vr.miniautorizador.cartao;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CartaoResponse")
class CartaoResponseTest {

    @Test
    @DisplayName("Deve copiar numero e senha do request quando montar a resposta")
    void deveCopiarCamposDoRequestQuandoMontarResposta() {
        final CartaoRequest request = new CartaoRequest("6549873025634501", "1234");

        final CartaoResponse resposta = CartaoResponse.de(request);

        assertThat(resposta.numeroCartao()).isEqualTo("6549873025634501");
        assertThat(resposta.senha()).isEqualTo("1234");
    }
}
