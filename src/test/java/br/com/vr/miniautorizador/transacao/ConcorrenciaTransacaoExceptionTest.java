package br.com.vr.miniautorizador.transacao;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;

@DisplayName("ConcorrenciaTransacaoException")
class ConcorrenciaTransacaoExceptionTest {

    @Test
    @DisplayName("Deve conter o numero do cartao na mensagem e preservar a causa quando criada")
    void deveConterNumeroEPreservarCausaQuandoCriada() {
        final OptimisticLockingFailureException causa = new OptimisticLockingFailureException("conflito");

        final ConcorrenciaTransacaoException excecao = new ConcorrenciaTransacaoException("6549873025634501", causa);

        assertThat(excecao.getMessage()).contains("6549873025634501");
        assertThat(excecao.getCause()).isSameAs(causa);
    }
}
