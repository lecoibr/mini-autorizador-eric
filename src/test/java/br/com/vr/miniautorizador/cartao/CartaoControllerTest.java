package br.com.vr.miniautorizador.cartao;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.vr.miniautorizador.seguranca.SegurancaConfig;
import br.com.vr.miniautorizador.seguranca.SegurancaProperties;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = CartaoController.class)
@Import(SegurancaConfig.class)
@EnableConfigurationProperties(SegurancaProperties.class)
@DisplayName("CartaoController")
class CartaoControllerTest {

    private static final String USUARIO = "username";
    private static final String SENHA_API = "password";
    private static final String NUMERO = "6549873025634501";
    private static final String SENHA_CARTAO = "1234";
    private static final String URL_CARTOES = "/cartoes";
    private static final String URL_CARTAO = "/cartoes/" + NUMERO;
    private static final String JSON_REQUEST = """
            {"numeroCartao":"6549873025634501","senha":"1234"}""";
    private static final String JSON_RESPOSTA = """
            {"senha":"1234","numeroCartao":"6549873025634501"}""";
    private static final String JSON_CAMPOS_ERRO = "$.erros[*].campo";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CartaoService service;

    @Nested
    @DisplayName("POST /cartoes")
    class Criar {

        @Test
        @DisplayName("Deve responder 201 com o JSON do contrato quando o cartao for criado")
        void deveResponder201QuandoCartaoCriado() throws Exception {
            final CartaoRequest request = new CartaoRequest(NUMERO, SENHA_CARTAO);
            when(service.criar(request)).thenReturn(new CartaoResponse(SENHA_CARTAO, NUMERO));

            mockMvc.perform(post(URL_CARTOES).with(httpBasic(USUARIO, SENHA_API))
                            .contentType(MediaType.APPLICATION_JSON).content(JSON_REQUEST))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(content().json(JSON_RESPOSTA, true));

            verify(service).criar(request);
        }

        @Test
        @DisplayName("Deve responder 422 com o mesmo JSON do request quando o cartao ja existir")
        void deveResponder422QuandoCartaoJaExistir() throws Exception {
            final CartaoRequest request = new CartaoRequest(NUMERO, SENHA_CARTAO);
            when(service.criar(request)).thenThrow(new CartaoJaExistenteException(request));

            mockMvc.perform(post(URL_CARTOES).with(httpBasic(USUARIO, SENHA_API))
                            .contentType(MediaType.APPLICATION_JSON).content(JSON_REQUEST))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(content().json(JSON_RESPOSTA, true));
        }

        @Test
        @DisplayName("Deve responder 400 listando os dois campos quando o corpo vier vazio")
        void deveResponder400QuandoCorpoVazio() throws Exception {
            mockMvc.perform(post(URL_CARTOES).with(httpBasic(USUARIO, SENHA_API))
                            .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath(JSON_CAMPOS_ERRO, containsInAnyOrder("numeroCartao", "senha")));

            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("Deve responder 400 apontando somente a senha quando ela vier em branco")
        void deveResponder400QuandoSenhaEmBranco() throws Exception {
            mockMvc.perform(post(URL_CARTOES).with(httpBasic(USUARIO, SENHA_API))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"numeroCartao\":\"" + NUMERO + "\",\"senha\":\"  \"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath(JSON_CAMPOS_ERRO, containsInAnyOrder("senha")));

            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("Deve responder 400 quando o JSON for malformado")
        void deveResponder400QuandoJsonMalformado() throws Exception {
            mockMvc.perform(post(URL_CARTOES).with(httpBasic(USUARIO, SENHA_API))
                            .contentType(MediaType.APPLICATION_JSON).content("{numeroCartao"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("Deve responder 401 sem acionar o service quando nao houver credenciais")
        void deveResponder401QuandoSemCredenciais() throws Exception {
            mockMvc.perform(post(URL_CARTOES).contentType(MediaType.APPLICATION_JSON).content(JSON_REQUEST))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("Deve responder 401 sem acionar o service quando as credenciais estiverem erradas")
        void deveResponder401QuandoCredenciaisErradas() throws Exception {
            mockMvc.perform(post(URL_CARTOES).with(httpBasic(USUARIO, "senha-errada"))
                            .contentType(MediaType.APPLICATION_JSON).content(JSON_REQUEST))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(service);
        }
    }

    @Nested
    @DisplayName("GET /cartoes/{numeroCartao}")
    class ConsultarSaldo {

        @Test
        @DisplayName("Deve responder 200 com o saldo como corpo quando o cartao existir")
        void deveResponder200ComSaldoQuandoCartaoExistir() throws Exception {
            when(service.consultarSaldo(NUMERO)).thenReturn(new BigDecimal("500.00"));

            mockMvc.perform(get(URL_CARTAO).with(httpBasic(USUARIO, SENHA_API)))
                    .andExpect(status().isOk())
                    .andExpect(content().string("500.00"));

            verify(service).consultarSaldo(NUMERO);
        }

        @Test
        @DisplayName("Deve responder 404 sem corpo quando o cartao nao existir")
        void deveResponder404SemCorpoQuandoCartaoNaoExistir() throws Exception {
            when(service.consultarSaldo(NUMERO)).thenThrow(new CartaoNaoEncontradoException(NUMERO));

            mockMvc.perform(get(URL_CARTAO).with(httpBasic(USUARIO, SENHA_API)))
                    .andExpect(status().isNotFound())
                    .andExpect(content().string(""));
        }

        @Test
        @DisplayName("Deve responder 401 sem acionar o service quando nao houver credenciais")
        void deveResponder401QuandoSemCredenciais() throws Exception {
            mockMvc.perform(get(URL_CARTAO))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("Deve responder 401 sem acionar o service quando as credenciais estiverem erradas")
        void deveResponder401QuandoCredenciaisErradas() throws Exception {
            mockMvc.perform(get(URL_CARTAO).with(httpBasic("outro-usuario", SENHA_API)))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("Deve repassar ao service exatamente o numero informado quando consultar saldo pela URL")
        void deveRepassarNumeroExatoAoServiceQuandoConsultarSaldoPelaUrl() throws Exception {
            when(service.consultarSaldo(any(String.class))).thenReturn(BigDecimal.ONE);

            mockMvc.perform(get(URL_CARTOES + "/0000111122223333").with(httpBasic(USUARIO, SENHA_API)))
                    .andExpect(status().isOk());

            verify(service).consultarSaldo("0000111122223333");
        }
    }
}
