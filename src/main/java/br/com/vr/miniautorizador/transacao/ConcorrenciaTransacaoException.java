package br.com.vr.miniautorizador.transacao;

/**
 * Lançada quando as tentativas de resolver o conflito de lock otimista se esgotam.
 */
public class ConcorrenciaTransacaoException extends RuntimeException {

    public ConcorrenciaTransacaoException(final String numeroCartao, final Throwable causa) {
        super("Conflito de concorrência ao debitar o cartão " + numeroCartao, causa);
    }
}
