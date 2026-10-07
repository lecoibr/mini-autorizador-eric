package br.com.vr.miniautorizador.transacao;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("TransacaoNaoAutorizadaException")
class TransacaoNaoAutorizadaExceptionTest {

    @ParameterizedTest(name = "{0}")
    @EnumSource(MotivoNaoAutorizacao.class)
    @DisplayName("Deve expor o motivo e citá-lo na mensagem quando criada")
    void deveExporMotivoEMensagemQuandoCriada(final MotivoNaoAutorizacao motivo) {
        final TransacaoNaoAutorizadaException excecao = new TransacaoNaoAutorizadaException(motivo);

        assertThat(excecao.getMotivo()).isEqualTo(motivo);
        assertThat(excecao.getMessage()).contains(motivo.name());
        assertThat(excecao.getCause()).isNull();
    }
}
