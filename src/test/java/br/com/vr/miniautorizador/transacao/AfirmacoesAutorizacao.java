package br.com.vr.miniautorizador.transacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

/**
 * Afirmações compartilhadas pelos testes das regras de autorização.
 */
final class AfirmacoesAutorizacao {

    private AfirmacoesAutorizacao() {
    }

    static void assertNaoAutorizadaPor(final ThrowingCallable acao, final MotivoNaoAutorizacao motivoEsperado) {
        assertThatThrownBy(acao)
                .isInstanceOfSatisfying(TransacaoNaoAutorizadaException.class,
                        excecao -> assertThat(excecao.getMotivo()).isEqualTo(motivoEsperado));
    }
}
