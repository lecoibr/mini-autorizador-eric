package br.com.vr.miniautorizador.transacao;

import static br.com.vr.miniautorizador.transacao.AfirmacoesAutorizacao.assertNaoAutorizadaPor;
import static org.assertj.core.api.Assertions.assertThatCode;

import br.com.vr.miniautorizador.cartao.Cartao;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("RegraSaldoSuficiente")
class RegraSaldoSuficienteTest {

    private final RegraSaldoSuficiente regra = new RegraSaldoSuficiente();

    private ContextoAutorizacao contextoComValor(final String valor) {
        final Cartao cartao = Cartao.criar("6549873025634501", "hash", new BigDecimal("500.00"));
        return new ContextoAutorizacao(Optional.of(cartao), "1234", new BigDecimal(valor));
    }

    @Test
    @DisplayName("Deve aprovar a regra quando o saldo for maior que o valor")
    void deveAprovarQuandoSaldoMaiorQueValor() {
        final ContextoAutorizacao contexto = contextoComValor("100.00");

        assertThatCode(() -> regra.validar(contexto)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Deve aprovar a regra quando o saldo for exatamente igual ao valor")
    void deveAprovarQuandoSaldoIgualAoValor() {
        final ContextoAutorizacao contexto = contextoComValor("500");

        assertThatCode(() -> regra.validar(contexto)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Deve barrar com SALDO_INSUFICIENTE quando o saldo for menor que o valor")
    void deveBarrarQuandoSaldoMenorQueValor() {
        final ContextoAutorizacao contexto = contextoComValor("500.01");

        assertNaoAutorizadaPor(() -> regra.validar(contexto), MotivoNaoAutorizacao.SALDO_INSUFICIENTE);
    }

    @Test
    @DisplayName("Deve barrar com CARTAO_INEXISTENTE quando o cartao nao existir")
    void deveBarrarQuandoCartaoNaoExistir() {
        final ContextoAutorizacao contexto = new ContextoAutorizacao(Optional.empty(), "1234", BigDecimal.TEN);

        assertNaoAutorizadaPor(() -> regra.validar(contexto), MotivoNaoAutorizacao.CARTAO_INEXISTENTE);
    }
}
