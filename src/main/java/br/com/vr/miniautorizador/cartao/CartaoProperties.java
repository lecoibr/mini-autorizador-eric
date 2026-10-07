package br.com.vr.miniautorizador.cartao;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Parâmetros de negócio do cartão, externalizados no application.yml. Validados na subida da aplicação,
 * para que uma configuração ausente falhe cedo e não só na primeira criação de cartão.
 *
 * @param saldoInicial saldo com que todo cartão novo é criado
 */
@Validated
@ConfigurationProperties(prefix = "mini-autorizador.cartao")
public record CartaoProperties(@NotNull @PositiveOrZero BigDecimal saldoInicial) {
}
