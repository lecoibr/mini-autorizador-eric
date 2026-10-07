package br.com.vr.miniautorizador.transacao;

import br.com.vr.miniautorizador.cartao.Cartao;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * Dados compartilhados pelas regras de autorização de uma transação.
 *
 * @param cartao         cartão encontrado para o número informado (vazio se inexistente)
 * @param senhaInformada senha em claro enviada na requisição
 * @param valor          valor da transação
 */
public record ContextoAutorizacao(Optional<Cartao> cartao, String senhaInformada, BigDecimal valor) {

    /**
     * Retorna o cartão ou interrompe a autorização por cartão inexistente.
     */
    public Cartao cartaoExistente() {
        return cartao.orElseThrow(() -> new TransacaoNaoAutorizadaException(MotivoNaoAutorizacao.CARTAO_INEXISTENTE));
    }
}
