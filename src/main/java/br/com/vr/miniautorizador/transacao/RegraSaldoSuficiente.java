package br.com.vr.miniautorizador.transacao;

import java.util.Optional;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Terceira regra: o saldo do cartão precisa cobrir o valor da transação.
 */
@Component
@Order(OrdemRegras.SALDO_SUFICIENTE)
public class RegraSaldoSuficiente implements RegraAutorizacao {

    @Override
    public void validar(final ContextoAutorizacao contexto) {
        Optional.of(contexto.cartaoExistente())
                .filter(cartao -> cartao.possuiSaldoPara(contexto.valor()))
                .orElseThrow(() -> new TransacaoNaoAutorizadaException(MotivoNaoAutorizacao.SALDO_INSUFICIENTE));
    }
}
