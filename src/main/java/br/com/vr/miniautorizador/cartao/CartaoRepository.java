package br.com.vr.miniautorizador.cartao;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acesso a dados dos cartões.
 */
public interface CartaoRepository extends JpaRepository<Cartao, Long> {

    Optional<Cartao> findByNumeroCartao(String numeroCartao);

    boolean existsByNumeroCartao(String numeroCartao);
}
