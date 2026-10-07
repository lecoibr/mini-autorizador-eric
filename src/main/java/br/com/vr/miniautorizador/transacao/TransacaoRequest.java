package br.com.vr.miniautorizador.transacao;

import br.com.vr.miniautorizador.cartao.CartaoRequest;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Corpo da requisição de autorização de transação.
 *
 * <p>O valor é limitado a duas casas decimais, a mesma escala da coluna {@code saldo DECIMAL(15,2)}: assim o
 * valor autorizado é exatamente o valor debitado, sem arredondamento silencioso no banco.</p>
 *
 * @param numeroCartao cartão usado como meio de pagamento
 * @param senhaCartao  senha do cartão em claro, conferida contra o hash armazenado
 * @param valor        valor a debitar, sempre positivo e com até duas casas decimais
 */
public record TransacaoRequest(
        @NotBlank @Size(max = CartaoRequest.TAMANHO_MAXIMO_NUMERO_CARTAO) String numeroCartao,
        @NotBlank @Size(max = CartaoRequest.TAMANHO_MAXIMO_SENHA) String senhaCartao,
        @NotNull @Positive @Digits(integer = DIGITOS_INTEIROS_VALOR, fraction = DIGITOS_DECIMAIS_VALOR) BigDecimal valor) {

    public static final int DIGITOS_INTEIROS_VALOR = 13;
    public static final int DIGITOS_DECIMAIS_VALOR = 2;
}
