package br.com.vr.miniautorizador.transacao;

/**
 * Regra de autorização de transação. Cada implementação é um bean ordenado por {@code @Order};
 * a primeira que falhar interrompe o processo lançando {@link TransacaoNaoAutorizadaException}.
 */
public interface RegraAutorizacao {

    /**
     * Valida o contexto, lançando {@link TransacaoNaoAutorizadaException} se a regra não for atendida.
     */
    void validar(ContextoAutorizacao contexto);
}
