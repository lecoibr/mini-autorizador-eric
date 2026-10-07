package br.com.vr.miniautorizador.transacao;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(controllers = TransacaoController.class)
@Import(SegurancaConfig.class)
@EnableConfigurationProperties(SegurancaProperties.class)
@DisplayName("TransacaoController")
class TransacaoControllerTest {

    private static final String USUARIO = "username";
    private static final String SENHA_API = "password";
    private static final String URL_TRANSACOES = "/transacoes";
    private static final String NUMERO = "6549873025634501";
    private static final String SENHA_CARTAO = "1234";
    private static final String JSON_VALIDO = """
            {"numeroCartao":"6549873025634501","senhaCartao":"1234","valor":10.00}""";
    private static final String MODELO_JSON_COM_VALOR = """
            {"numeroCartao":"6549873025634501","senhaCartao":"1234","valor":%s}""";
    private static final String JSON_CAMPOS_ERRO = "$.erros[*].campo";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransacaoService service;

    private MockHttpServletRequestBuilder requisicaoAutenticada(final String corpo) {
        return post(URL_TRANSACOES).with(httpBasic(USUARIO, SENHA_API))
                .contentType(MediaType.APPLICATION_JSON).content(corpo);
    }

    @Nested
    @DisplayName("Transacao autorizada")
    class Autorizada {

        @Test
        @DisplayName("Deve responder 201 com corpo OK em texto puro quando a transacao for autorizada")
        void deveResponder201ComOkQuandoAutorizada() throws Exception {
            mockMvc.perform(requisicaoAutenticada(JSON_VALIDO))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                    .andExpect(content().string("OK"));

            verify(service).autorizar(new TransacaoRequest(NUMERO, SENHA_CARTAO, new BigDecimal("10.00")));
        }

        @Test
        @DisplayName("Deve responder 201 com corpo OK e nao 406 quando o cliente enviar Accept application/json")
        void deveResponder201QuandoAcceptJson() throws Exception {
            mockMvc.perform(requisicaoAutenticada(JSON_VALIDO).accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isCreated())
                    .andExpect(content().string("OK"));
        }
    }

    @Nested
    @DisplayName("Transacao nao autorizada")
    class NaoAutorizada {

        @ParameterizedTest(name = "{0}")
        @EnumSource(MotivoNaoAutorizacao.class)
        @DisplayName("Deve responder 422 com o nome do motivo como corpo quando a regra barrar")
        void deveResponder422ComMotivoQuandoRegraBarrar(final MotivoNaoAutorizacao motivo) throws Exception {
            doThrow(new TransacaoNaoAutorizadaException(motivo)).when(service).autorizar(any(TransacaoRequest.class));

            mockMvc.perform(requisicaoAutenticada(JSON_VALIDO))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                    .andExpect(content().string(motivo.name()));
        }

        @Test
        @DisplayName("Deve responder 409 com CONFLITO_CONCORRENCIA quando as tentativas se esgotarem")
        void deveResponder409QuandoConcorrencia() throws Exception {
            doThrow(new ConcorrenciaTransacaoException(NUMERO, new OptimisticLockingFailureException("conflito")))
                    .when(service).autorizar(any(TransacaoRequest.class));

            mockMvc.perform(requisicaoAutenticada(JSON_VALIDO))
                    .andExpect(status().isConflict())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                    .andExpect(content().string("CONFLITO_CONCORRENCIA"));
        }
    }

    @Nested
    @DisplayName("Validacao e autenticacao")
    class ValidacaoEAutenticacao {

        @ParameterizedTest(name = "valor = {0}")
        @ValueSource(strings = {"-10.00", "0", "0.00", "null"})
        @DisplayName("Deve responder 400 apontando o campo valor quando ele for negativo, zero ou nulo")
        void deveResponder400QuandoValorInvalido(final String valor) throws Exception {
            mockMvc.perform(requisicaoAutenticada(MODELO_JSON_COM_VALOR.formatted(valor)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath(JSON_CAMPOS_ERRO, contains("valor")));

            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("Deve responder 400 apontando o campo valor quando ele estiver ausente")
        void deveResponder400QuandoValorAusente() throws Exception {
            final String semValor = "{\"numeroCartao\":\"" + NUMERO + "\",\"senhaCartao\":\"" + SENHA_CARTAO + "\"}";

            mockMvc.perform(requisicaoAutenticada(semValor))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath(JSON_CAMPOS_ERRO, contains("valor")));

            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("Deve responder 400 listando numero e senha do cartao quando vierem em branco")
        void deveResponder400QuandoNumeroESenhaEmBranco() throws Exception {
            final String corpo = "{\"numeroCartao\":\"\",\"senhaCartao\":\"\",\"valor\":10.00}";

            mockMvc.perform(requisicaoAutenticada(corpo))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath(JSON_CAMPOS_ERRO, containsInAnyOrder("numeroCartao", "senhaCartao")));

            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("Deve responder 401 sem acionar o service quando nao houver credenciais")
        void deveResponder401QuandoSemCredenciais() throws Exception {
            mockMvc.perform(post(URL_TRANSACOES).contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("Deve responder 401 sem acionar o service quando a senha da API estiver errada")
        void deveResponder401QuandoCredenciaisErradas() throws Exception {
            mockMvc.perform(post(URL_TRANSACOES).with(httpBasic(USUARIO, "errada"))
                            .contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(service);
        }
    }
}
