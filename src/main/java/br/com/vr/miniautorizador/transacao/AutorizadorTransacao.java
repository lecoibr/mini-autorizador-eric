package br.com.vr.miniautorizador.transacao;

import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Executa em sequência todas as {@link RegraAutorizacao}. A lista é injetada pelo Spring já ordenada
 * por {@code @Order}, então acrescentar uma regra nova não exige alterar esta classe.
 */
@Component
public class AutorizadorTransacao {

    private final List<RegraAutorizacao> regras;

    public AutorizadorTransacao(final List<RegraAutorizacao> regras) {
        this.regras = List.copyOf(regras);
    }

    /**
     * Aplica as regras ao contexto; a primeira violação interrompe com {@link TransacaoNaoAutorizadaException}.
     */
    public void autorizar(final ContextoAutorizacao contexto) {
        regras.forEach(regra -> regra.validar(contexto));
    }
}
