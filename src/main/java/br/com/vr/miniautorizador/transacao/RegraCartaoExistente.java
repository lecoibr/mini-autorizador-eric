package br.com.vr.miniautorizador.transacao;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Primeira regra: o cartão informado precisa existir.
 */
@Component
@Order(OrdemRegras.CARTAO_EXISTENTE)
public class RegraCartaoExistente implements RegraAutorizacao {

    @Override
    public void validar(final ContextoAutorizacao contexto) {
        contexto.cartaoExistente();
    }
}
