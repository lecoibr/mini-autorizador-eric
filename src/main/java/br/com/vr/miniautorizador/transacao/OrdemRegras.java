package br.com.vr.miniautorizador.transacao;

/**
 * Ordem de execução das regras de autorização. Centralizada aqui porque a sequência faz parte do contrato:
 * um cartão inexistente responde CARTAO_INEXISTENTE (e não SENHA_INVALIDA), e uma senha errada responde
 * SENHA_INVALIDA mesmo que também falte saldo.
 */
public final class OrdemRegras {

    public static final int CARTAO_EXISTENTE = 1;
    public static final int SENHA_VALIDA = 2;
    public static final int SALDO_SUFICIENTE = 3;

    private OrdemRegras() {
        // Classe utilitária de constantes.
    }
}
