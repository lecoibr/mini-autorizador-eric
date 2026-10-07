package br.com.vr.miniautorizador.comum;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.vr.miniautorizador.cartao.CartaoJaExistenteException;
import br.com.vr.miniautorizador.cartao.CartaoRequest;
import br.com.vr.miniautorizador.cartao.CartaoResponse;
import br.com.vr.miniautorizador.transacao.ConcorrenciaTransacaoException;
import br.com.vr.miniautorizador.transacao.MotivoNaoAutorizacao;
import br.com.vr.miniautorizador.transacao.TransacaoNaoAutorizadaException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

@DisplayName("TratadorGlobalDeErros")
class TratadorGlobalDeErrosTest {

    private static final String NUMERO = "6549873025634501";
    private static final String SENHA = "1234";
    private static final String OBJETO = "cartaoRequest";

    private final TratadorGlobalDeErros tratador = new TratadorGlobalDeErros();

    /** Método apenas para fornecer um {@link MethodParameter} real à exceção de validação. */
    @SuppressWarnings("unused")
    private void metodoAlvo(final CartaoRequest request) {
        // Sem corpo: serve somente de alvo de reflexão.
    }

    @Test
    @DisplayName("Deve responder 422 com JSON ecoando o request quando o cartao ja existir")
    void deveResponder422ComJsonQuandoCartaoJaExistir() {
        final CartaoJaExistenteException excecao = new CartaoJaExistenteException(new CartaoRequest(NUMERO, SENHA));

        final ResponseEntity<CartaoResponse> resposta = tratador.tratarCartaoJaExistente(excecao);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(resposta.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(resposta.getBody()).isEqualTo(new CartaoResponse(SENHA, NUMERO));
    }

    @Test
    @DisplayName("Deve responder 404 sem corpo quando o cartao nao for encontrado")
    void deveResponder404SemCorpoQuandoCartaoNaoEncontrado() {
        final ResponseEntity<Void> resposta = tratador.tratarCartaoNaoEncontrado();

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resposta.getBody()).isNull();
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(MotivoNaoAutorizacao.class)
    @DisplayName("Deve responder 422 com o nome do motivo em texto puro quando a transacao nao for autorizada")
    void deveResponder422ComMotivoQuandoTransacaoNaoAutorizada(final MotivoNaoAutorizacao motivo) {
        final ResponseEntity<String> resposta =
                tratador.tratarTransacaoNaoAutorizada(new TransacaoNaoAutorizadaException(motivo));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(resposta.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
        assertThat(resposta.getBody()).isEqualTo(motivo.name());
    }

    @Test
    @DisplayName("Deve responder 409 com CONFLITO_CONCORRENCIA em texto puro quando houver conflito de concorrencia")
    void deveResponder409QuandoConflitoDeConcorrencia() {
        final ResponseEntity<String> resposta = tratador.tratarConcorrencia();

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
        assertThat(resposta.getBody()).isEqualTo("CONFLITO_CONCORRENCIA");
    }

    @Test
    @DisplayName("Deve responder 400 listando campo e mensagem de cada erro quando a validacao falhar")
    void deveResponder400ListandoErrosQuandoValidacaoFalhar() throws NoSuchMethodException {
        final BindingResult bindingResult = new BeanPropertyBindingResult(new CartaoRequest(null, null), OBJETO);
        bindingResult.addError(new FieldError(OBJETO, "numeroCartao", "nao pode estar em branco"));
        bindingResult.addError(new FieldError(OBJETO, "senha", "tamanho invalido"));
        final MethodParameter parametro = new MethodParameter(
                TratadorGlobalDeErrosTest.class.getDeclaredMethod("metodoAlvo", CartaoRequest.class), 0);

        final ResponseEntity<ErroValidacaoResponse> resposta =
                tratador.tratarValidacao(new MethodArgumentNotValidException(parametro, bindingResult));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(resposta.getBody()).isNotNull();
        assertThat(resposta.getBody().erros()).containsExactly(
                new ErroValidacaoResponse.CampoInvalido("numeroCartao", "nao pode estar em branco"),
                new ErroValidacaoResponse.CampoInvalido("senha", "tamanho invalido"));
    }

    @Test
    @DisplayName("Deve responder 400 com lista vazia quando nao houver erros de campo")
    void deveResponder400ComListaVaziaQuandoNaoHouverErrosDeCampo() throws NoSuchMethodException {
        final BindingResult bindingResult = new BeanPropertyBindingResult(new CartaoRequest(NUMERO, SENHA), OBJETO);
        final MethodParameter parametro = new MethodParameter(
                TratadorGlobalDeErrosTest.class.getDeclaredMethod("metodoAlvo", CartaoRequest.class), 0);

        final ResponseEntity<ErroValidacaoResponse> resposta =
                tratador.tratarValidacao(new MethodArgumentNotValidException(parametro, bindingResult));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).isNotNull();
        assertThat(resposta.getBody().erros()).isEmpty();
    }

    @Test
    @DisplayName("Deve omitir o numero do cartao no corpo do 409 mesmo que a excecao o contenha na mensagem")
    void deveOmitirNumeroDoCartaoQuandoConflitoDeConcorrencia() {
        final ConcorrenciaTransacaoException excecao = new ConcorrenciaTransacaoException(NUMERO, null);

        assertThat(excecao.getMessage()).contains(NUMERO);
        assertThat(tratador.tratarConcorrencia().getBody()).isNotBlank().doesNotContain(NUMERO);
    }
}
