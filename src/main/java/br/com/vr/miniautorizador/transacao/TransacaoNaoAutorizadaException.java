package br.com.vr.miniautorizador.transacao;

/**
 * Lançada pela regra de autorização que barrou a transação, informando o motivo.
 */
public class TransacaoNaoAutorizadaException extends RuntimeException {

    private final MotivoNaoAutorizacao motivo;

    public TransacaoNaoAutorizadaException(final MotivoNaoAutorizacao motivo) {
        super("Transação não autorizada: " + motivo);
        this.motivo = motivo;
    }

    public MotivoNaoAutorizacao getMotivo() {
        return motivo;
    }
}
