package br.com.vr.miniautorizador.transacao;

/**
 * Motivos pelos quais uma transação deixa de ser autorizada. O nome do enum é o corpo da resposta 422.
 */
public enum MotivoNaoAutorizacao {
    SALDO_INSUFICIENTE,
    SENHA_INVALIDA,
    CARTAO_INEXISTENTE
}
