package br.com.vr.miniautorizador.transacao;

import static br.com.vr.miniautorizador.transacao.AfirmacoesAutorizacao.assertNaoAutorizadaPor;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.vr.miniautorizador.cartao.Cartao;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@DisplayName("RegraSenhaValida")
class RegraSenhaValidaTest {

    private static final String SENHA_INFORMADA = "1234";
    private static final String HASH_DO_CARTAO = "hash-armazenado";

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private RegraSenhaValida regra;

    private ContextoAutorizacao contextoComCartao() {
        final Cartao cartao = Cartao.criar("6549873025634501", HASH_DO_CARTAO, new BigDecimal("500.00"));
        return new ContextoAutorizacao(Optional.of(cartao), SENHA_INFORMADA, BigDecimal.TEN);
    }

    @Test
    @DisplayName("Deve aprovar a regra e comparar senha informada com o hash do cartao quando a senha conferir")
    void deveAprovarQuandoSenhaConferir() {
        final ContextoAutorizacao contexto = contextoComCartao();
        when(passwordEncoder.matches(SENHA_INFORMADA, HASH_DO_CARTAO)).thenReturn(true);

        assertThatCode(() -> regra.validar(contexto)).doesNotThrowAnyException();

        verify(passwordEncoder).matches(SENHA_INFORMADA, HASH_DO_CARTAO);
    }

    @Test
    @DisplayName("Deve barrar com SENHA_INVALIDA quando a senha nao conferir")
    void deveBarrarQuandoSenhaNaoConferir() {
        final ContextoAutorizacao contexto = contextoComCartao();
        when(passwordEncoder.matches(SENHA_INFORMADA, HASH_DO_CARTAO)).thenReturn(false);

        assertNaoAutorizadaPor(() -> regra.validar(contexto), MotivoNaoAutorizacao.SENHA_INVALIDA);

        verify(passwordEncoder).matches(SENHA_INFORMADA, HASH_DO_CARTAO);
    }

    @Test
    @DisplayName("Deve barrar com CARTAO_INEXISTENTE sem consultar o encoder quando o cartao nao existir")
    void deveBarrarSemConsultarEncoderQuandoCartaoNaoExistir() {
        final ContextoAutorizacao contexto = new ContextoAutorizacao(Optional.empty(), SENHA_INFORMADA, BigDecimal.TEN);

        assertNaoAutorizadaPor(() -> regra.validar(contexto), MotivoNaoAutorizacao.CARTAO_INEXISTENTE);

        verifyNoInteractions(passwordEncoder);
    }
}
