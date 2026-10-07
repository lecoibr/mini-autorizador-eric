package br.com.vr.miniautorizador.cartao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@DisplayName("CartaoService")
class CartaoServiceTest {

    private static final String NUMERO = "6549873025634501";
    private static final String SENHA = "1234";
    private static final String HASH_SENHA = "hash-bcrypt-simulado";
    private static final BigDecimal SALDO_INICIAL = new BigDecimal("500.00");

    @Mock
    private CartaoRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private CartaoService service;

    @BeforeEach
    void configurar() {
        service = new CartaoService(repository, passwordEncoder, new CartaoProperties(SALDO_INICIAL));
    }

    @Nested
    @DisplayName("criar")
    class Criar {

        @Test
        @DisplayName("Deve persistir cartao com saldo inicial e senha criptografada quando numero estiver livre")
        void devePersistirCartaoQuandoNumeroLivre() {
            final CartaoRequest request = new CartaoRequest(NUMERO, SENHA);
            when(repository.existsByNumeroCartao(NUMERO)).thenReturn(false);
            when(passwordEncoder.encode(SENHA)).thenReturn(HASH_SENHA);

            final CartaoResponse resposta = service.criar(request);

            final ArgumentCaptor<Cartao> captor = ArgumentCaptor.forClass(Cartao.class);
            verify(repository).saveAndFlush(captor.capture());
            final Cartao persistido = captor.getValue();
            assertThat(persistido.getNumeroCartao()).isEqualTo(NUMERO);
            assertThat(persistido.getSaldo()).isEqualTo(SALDO_INICIAL);
            assertThat(persistido.getSenha()).isEqualTo(HASH_SENHA);
            assertThat(resposta).isEqualTo(new CartaoResponse(SENHA, NUMERO));
        }

        @Test
        @DisplayName("Deve lancar CartaoJaExistenteException sem gravar quando o numero ja existir")
        void deveLancarExcecaoSemGravarQuandoNumeroJaExistir() {
            final CartaoRequest request = new CartaoRequest(NUMERO, SENHA);
            when(repository.existsByNumeroCartao(NUMERO)).thenReturn(true);

            assertThatThrownBy(() -> service.criar(request))
                    .isInstanceOfSatisfying(CartaoJaExistenteException.class,
                            excecao -> assertThat(excecao.getRequest()).isEqualTo(request))
                    .hasMessageContaining(NUMERO);

            verify(repository, never()).saveAndFlush(any(Cartao.class));
        }

        @Test
        @DisplayName("Deve lancar CartaoJaExistenteException com a causa original quando a constraint unica for violada")
        void deveLancarExcecaoComCausaQuandoConstraintUnicaViolada() {
            final CartaoRequest request = new CartaoRequest(NUMERO, SENHA);
            final DataIntegrityViolationException violacao = new DataIntegrityViolationException("uk_cartao_numero_cartao");
            when(repository.existsByNumeroCartao(NUMERO)).thenReturn(false);
            when(passwordEncoder.encode(SENHA)).thenReturn(HASH_SENHA);
            when(repository.saveAndFlush(any(Cartao.class))).thenThrow(violacao);

            assertThatThrownBy(() -> service.criar(request))
                    .isInstanceOfSatisfying(CartaoJaExistenteException.class,
                            excecao -> assertThat(excecao.getRequest()).isEqualTo(request))
                    .hasCause(violacao);
        }
    }

    @Nested
    @DisplayName("consultarSaldo")
    class ConsultarSaldo {

        @Test
        @DisplayName("Deve retornar o saldo do cartao quando ele existir")
        void deveRetornarSaldoQuandoCartaoExistir() {
            final Cartao cartao = Cartao.criar(NUMERO, HASH_SENHA, new BigDecimal("495.15"));
            when(repository.findByNumeroCartao(NUMERO)).thenReturn(Optional.of(cartao));

            assertThat(service.consultarSaldo(NUMERO)).isEqualTo(new BigDecimal("495.15"));
        }

        @Test
        @DisplayName("Deve lancar CartaoNaoEncontradoException quando o cartao nao existir")
        void deveLancarExcecaoQuandoCartaoNaoExistir() {
            when(repository.findByNumeroCartao(NUMERO)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.consultarSaldo(NUMERO))
                    .isInstanceOf(CartaoNaoEncontradoException.class)
                    .hasMessageContaining(NUMERO);
        }
    }
}
