package br.com.vr.miniautorizador.cartao;

import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso de cartão: criação (com saldo inicial configurável e senha armazenada como hash)
 * e consulta de saldo.
 */
@Service
public class CartaoService {

    private final CartaoRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final CartaoProperties properties;

    public CartaoService(final CartaoRepository repository, final PasswordEncoder passwordEncoder,
                         final CartaoProperties properties) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    /**
     * Cria um cartão novo. A checagem prévia cobre o caso comum; a captura da violação da constraint única
     * cobre a corrida entre dois POSTs simultâneos com o mesmo número, que passariam juntos pela checagem.
     *
     * @throws CartaoJaExistenteException se o número já estiver cadastrado
     */
    @Transactional
    public CartaoResponse criar(final CartaoRequest request) {
        garantirNumeroDisponivel(request);
        try {
            repository.saveAndFlush(montarCartao(request));
        } catch (DataIntegrityViolationException e) {
            throw new CartaoJaExistenteException(request, e);
        }
        return CartaoResponse.de(request);
    }

    /**
     * Retorna o saldo atual do cartão.
     *
     * @throws CartaoNaoEncontradoException se o cartão não existir
     */
    @Transactional(readOnly = true)
    public BigDecimal consultarSaldo(final String numeroCartao) {
        return repository.findByNumeroCartao(numeroCartao)
                .map(Cartao::getSaldo)
                .orElseThrow(() -> new CartaoNaoEncontradoException(numeroCartao));
    }

    private void garantirNumeroDisponivel(final CartaoRequest request) {
        Optional.of(request)
                .filter(r -> !repository.existsByNumeroCartao(r.numeroCartao()))
                .orElseThrow(() -> new CartaoJaExistenteException(request));
    }

    private Cartao montarCartao(final CartaoRequest request) {
        return Cartao.criar(request.numeroCartao(), passwordEncoder.encode(request.senha()),
                properties.saldoInicial());
    }
}
