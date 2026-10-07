package br.com.vr.miniautorizador.seguranca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.vr.miniautorizador.cartao.CartaoController;
import br.com.vr.miniautorizador.cartao.CartaoService;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = CartaoController.class)
@Import(SegurancaConfig.class)
@EnableConfigurationProperties(SegurancaProperties.class)
@DisplayName("SegurancaConfig")
class SegurancaConfigTest {

    private static final String USUARIO = "username";
    private static final String SENHA_API = "password";
    private static final String URL_API_DOCS = "/v3/api-docs";
    private static final String URL_HEALTH = "/actuator/health";
    private static final String NUMERO_CARTAO = "6549873025634501";
    private static final String SENHA_CARTAO = "1234";
    private static final String URL_SALDO = "/cartoes/" + NUMERO_CARTAO;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private CartaoService cartaoService;

    @Nested
    @DisplayName("Rotas publicas")
    class RotasPublicas {

        @Test
        @DisplayName("Deve nao exigir autenticacao quando acessar a documentacao OpenAPI via GET")
        void deveNaoExigirAutenticacaoQuandoAcessarApiDocs() throws Exception {
            mockMvc.perform(get(URL_API_DOCS)).andExpect(this::passouPelaSegurancaSemCredencial);
        }

        @Test
        @DisplayName("Deve nao exigir autenticacao quando acessar o health check via GET")
        void deveNaoExigirAutenticacaoQuandoAcessarHealth() throws Exception {
            mockMvc.perform(get(URL_HEALTH)).andExpect(this::passouPelaSegurancaSemCredencial);
        }

        /**
         * No slice {@code @WebMvcTest} o springdoc e o actuator nao estao carregados, entao a rota publica
         * responde 404 (ou 200 se existir). O que se prova aqui e o {@code permitAll}: nunca 401 nem 403.
         */
        private void passouPelaSegurancaSemCredencial(final MvcResult resultado) {
            assertThat(resultado.getResponse().getStatus())
                    .isIn(HttpStatus.OK.value(), HttpStatus.NOT_FOUND.value());
        }

        @Test
        @DisplayName("Deve exigir autenticacao quando usar metodo diferente de GET nas rotas publicas")
        void deveExigirAutenticacaoQuandoMetodoNaoForGet() throws Exception {
            mockMvc.perform(post(URL_API_DOCS))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Rotas protegidas")
    class RotasProtegidas {

        @Test
        @DisplayName("Deve aceitar o usuario configurado no application.yml quando as credenciais conferirem")
        void deveAceitarUsuarioConfiguradoQuandoCredenciaisConferirem() throws Exception {
            when(cartaoService.consultarSaldo(NUMERO_CARTAO)).thenReturn(new BigDecimal("500.00"));

            mockMvc.perform(get(URL_SALDO).with(httpBasic(USUARIO, SENHA_API)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Deve responder 401 quando o usuario existir mas a senha estiver errada")
        void deveResponder401QuandoSenhaErrada() throws Exception {
            mockMvc.perform(get(URL_SALDO).with(httpBasic(USUARIO, "errada")))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Deve responder 401 quando o usuario nao existir")
        void deveResponder401QuandoUsuarioInexistente() throws Exception {
            mockMvc.perform(get(URL_SALDO).with(httpBasic("desconhecido", SENHA_API)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Deve responder 401 quando nao houver credenciais")
        void deveResponder401QuandoSemCredenciais() throws Exception {
            mockMvc.perform(get(URL_SALDO))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("PasswordEncoder")
    class Codificador {

        @Test
        @DisplayName("Deve ser BCrypt e nunca devolver a senha em claro quando codificar")
        void deveSerBcryptQuandoCodificar() {
            final String hash = passwordEncoder.encode(SENHA_CARTAO);

            assertThat(passwordEncoder).isInstanceOf(BCryptPasswordEncoder.class);
            assertThat(hash).isNotEqualTo(SENHA_CARTAO).startsWith("$2");
            assertThat(passwordEncoder.matches(SENHA_CARTAO, hash)).isTrue();
            assertThat(passwordEncoder.matches("4321", hash)).isFalse();
        }

        @Test
        @DisplayName("Deve gerar hashes diferentes para a mesma senha quando codificar duas vezes")
        void deveGerarHashesDiferentesQuandoCodificarDuasVezes() {
            assertThat(passwordEncoder.encode(SENHA_CARTAO)).isNotEqualTo(passwordEncoder.encode(SENHA_CARTAO));
        }
    }
}
