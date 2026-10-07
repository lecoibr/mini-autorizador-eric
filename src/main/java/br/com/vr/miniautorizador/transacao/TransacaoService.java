package br.com.vr.miniautorizador.transacao;

import br.com.vr.miniautorizador.cartao.Cartao;
import br.com.vr.miniautorizador.cartao.CartaoRepository;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autoriza transações e debita o saldo do cartão.
 *
 * <p>Concorrência: o cartão tem lock otimista ({@code @Version}). Se duas transações leem o mesmo saldo e
 * ambas tentam gravar, a segunda falha no flush com erro de lock otimista. O {@code @Retryable} refaz o método
 * inteiro em uma nova transação: o cartão é relido e todas as regras são reavaliadas com o saldo já debitado.
 * Por isso a segunda transação concorrente normalmente termina em SALDO_INSUFICIENTE já na retentativa, e o
 * conflito (HTTP 409) só ocorre sob contenção extrema, quando as tentativas se esgotam.</p>
 */
@Service
public class TransacaoService {

    // Cada rodada de conflito tem um único vencedor; com vários concorrentes no mesmo cartão, poucas tentativas
    // e espera fixa fariam todos colidirem de novo. Por isso: mais tentativas e espera aleatória (jitter).
    private static final int MAXIMO_TENTATIVAS = 5;
    private static final long ATRASO_INICIAL_MS = 20;
    private static final long ATRASO_MAXIMO_MS = 200;
    private static final double MULTIPLICADOR_ATRASO = 2;

    private final CartaoRepository repository;
    private final AutorizadorTransacao autorizador;

    public TransacaoService(final CartaoRepository repository, final AutorizadorTransacao autorizador) {
        this.repository = repository;
        this.autorizador = autorizador;
    }

    /**
     * Valida as regras de autorização e debita o valor. O {@code saveAndFlush} força a checagem de versão
     * dentro do método, para que o conflito seja detectado aqui e retentado, e não só no commit.
     *
     * @throws TransacaoNaoAutorizadaException se alguma regra barrar a transação
     * @throws ConcorrenciaTransacaoException  se o conflito de concorrência persistir após todas as tentativas
     */
    @Transactional
    @Retryable(
            // OptimisticLockingFailureException é a raiz da hierarquia: cobre também a ObjectOptimisticLockingFailureException
            // que o Spring Data gera ao traduzir a StaleObjectStateException do Hibernate.
            retryFor = OptimisticLockingFailureException.class,
            maxAttempts = MAXIMO_TENTATIVAS,
            backoff = @Backoff(delay = ATRASO_INICIAL_MS, maxDelay = ATRASO_MAXIMO_MS,
                    multiplier = MULTIPLICADOR_ATRASO, random = true))
    public void autorizar(final TransacaoRequest request) {
        final ContextoAutorizacao contexto = new ContextoAutorizacao(
                repository.findByNumeroCartao(request.numeroCartao()), request.senhaCartao(), request.valor());
        autorizador.autorizar(contexto);
        final Cartao cartaoAutorizado = contexto.cartaoExistente();
        cartaoAutorizado.debitar(request.valor());
        repository.saveAndFlush(cartaoAutorizado);
    }

    /**
     * Chamado quando as tentativas se esgotam por conflito de concorrência.
     */
    @Recover
    public void recuperar(final OptimisticLockingFailureException causa, final TransacaoRequest request) {
        throw new ConcorrenciaTransacaoException(request.numeroCartao(), causa);
    }

    /**
     * Chamado quando uma tentativa termina com exceção que não é de concorrência, como uma regra de autorização
     * negada na retentativa (o caso típico: a transação concorrente consumiu o saldo). Apenas a propaga.
     *
     * <p>Necessário porque, havendo métodos {@code @Recover}, o spring-retry exige um compatível com o tipo lançado;
     * sem este, a negação de negócio seria convertida em {@code ExhaustedRetryException} (HTTP 500) em vez de 422.
     * O spring-retry escolhe o método mais específico, então conflitos continuam caindo em {@link #recuperar}.</p>
     */
    @Recover
    public void propagarFalhaNaoRetentavel(final RuntimeException causa, final TransacaoRequest request) {
        throw causa;
    }
}
