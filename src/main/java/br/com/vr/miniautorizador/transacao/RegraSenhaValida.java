package br.com.vr.miniautorizador.transacao;

import java.util.Optional;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Segunda regra: a senha informada precisa corresponder ao hash armazenado no cartão.
 */
@Component
@Order(OrdemRegras.SENHA_VALIDA)
public class RegraSenhaValida implements RegraAutorizacao {

    private final PasswordEncoder passwordEncoder;

    public RegraSenhaValida(final PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void validar(final ContextoAutorizacao contexto) {
        Optional.of(contexto.cartaoExistente())
                .filter(cartao -> passwordEncoder.matches(contexto.senhaInformada(), cartao.getSenha()))
                .orElseThrow(() -> new TransacaoNaoAutorizadaException(MotivoNaoAutorizacao.SENHA_INVALIDA));
    }
}
