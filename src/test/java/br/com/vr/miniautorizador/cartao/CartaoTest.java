package br.com.vr.miniautorizador.cartao;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Cartao")
class CartaoTest {

    private static final String NUMERO = "6549873025634501";
    private static final String HASH_SENHA = "hash-da-senha";
    private static final BigDecimal SALDO_INICIAL = new BigDecimal("500.00");

    private Cartao novoCartao() {
        return Cartao.criar(NUMERO, HASH_SENHA, SALDO_INICIAL);
    }

    @Test
    @DisplayName("Deve inicializar numero, senha e saldo e deixar a versao nula quando criar o cartao")
    void deveInicializarDadosQuandoCriarCartao() {
        final Cartao cartao = novoCartao();

        assertThat(cartao.getNumeroCartao()).isEqualTo(NUMERO);
        assertThat(cartao.getSenha()).isEqualTo(HASH_SENHA);
        assertThat(cartao.getSaldo()).isEqualTo(SALDO_INICIAL);
        assertThat(cartao.getVersao()).isNull();
        assertThat(cartao.getId()).isNull();
    }

    @Nested
    @DisplayName("possuiSaldoPara")
    class PossuiSaldoPara {

        @Test
        @DisplayName("Deve retornar verdadeiro quando o valor for menor que o saldo")
        void deveRetornarVerdadeiroQuandoValorMenorQueSaldo() {
            assertThat(novoCartao().possuiSaldoPara(new BigDecimal("499.99"))).isTrue();
        }

        @Test
        @DisplayName("Deve retornar verdadeiro quando o valor for igual ao saldo, mesmo com escala diferente")
        void deveRetornarVerdadeiroQuandoValorIgualAoSaldo() {
            assertThat(novoCartao().possuiSaldoPara(new BigDecimal("500"))).isTrue();
        }

        @Test
        @DisplayName("Deve retornar falso quando o valor for maior que o saldo")
        void deveRetornarFalsoQuandoValorMaiorQueSaldo() {
            assertThat(novoCartao().possuiSaldoPara(new BigDecimal("500.01"))).isFalse();
        }
    }

    @Nested
    @DisplayName("debitar")
    class Debitar {

        @Test
        @DisplayName("Deve subtrair o valor do saldo mantendo a escala quando debitar")
        void deveSubtrairValorMantendoEscalaQuandoDebitar() {
            final Cartao cartao = novoCartao();

            cartao.debitar(new BigDecimal("100.00"));

            assertThat(cartao.getSaldo()).isEqualTo(new BigDecimal("400.00"));
            assertThat(cartao.getSaldo().scale()).isEqualTo(2);
        }

        @Test
        @DisplayName("Deve manter duas casas decimais quando debitar valor sem casas decimais")
        void deveManterEscalaQuandoDebitarValorInteiro() {
            final Cartao cartao = novoCartao();

            cartao.debitar(new BigDecimal("10"));

            assertThat(cartao.getSaldo()).isEqualTo(new BigDecimal("490.00"));
        }

        @Test
        @DisplayName("Deve zerar o saldo quando debitar exatamente o saldo disponivel")
        void deveZerarSaldoQuandoDebitarSaldoTotal() {
            final Cartao cartao = novoCartao();

            cartao.debitar(SALDO_INICIAL);

            assertThat(cartao.getSaldo()).isEqualTo(new BigDecimal("0.00"));
            assertThat(cartao.possuiSaldoPara(new BigDecimal("0.01"))).isFalse();
        }
    }
}
