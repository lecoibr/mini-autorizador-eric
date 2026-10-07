package br.com.vr.miniautorizador.comum;

import br.com.vr.miniautorizador.cartao.CartaoJaExistenteException;
import br.com.vr.miniautorizador.cartao.CartaoNaoEncontradoException;
import br.com.vr.miniautorizador.cartao.CartaoResponse;
import br.com.vr.miniautorizador.transacao.ConcorrenciaTransacaoException;
import br.com.vr.miniautorizador.transacao.TransacaoNaoAutorizadaException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduz as exceções de domínio nos status e corpos definidos pelo contrato da API.
 */
@RestControllerAdvice
public class TratadorGlobalDeErros {

    private static final String CORPO_CONFLITO_CONCORRENCIA = "CONFLITO_CONCORRENCIA";
    private static final String CAMPO_CORPO_REQUISICAO = "corpo";
    private static final String MENSAGEM_CORPO_ILEGIVEL = "corpo da requisição ausente ou JSON inválido";

    /**
     * Cartão duplicado: 422 devolvendo os mesmos dados da requisição, como exige o contrato.
     */
    @ExceptionHandler(CartaoJaExistenteException.class)
    public ResponseEntity<CartaoResponse> tratarCartaoJaExistente(final CartaoJaExistenteException excecao) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .contentType(MediaType.APPLICATION_JSON)
                .body(CartaoResponse.de(excecao.getRequest()));
    }

    /**
     * Consulta de saldo de cartão inexistente: 404 sem corpo.
     */
    @ExceptionHandler(CartaoNaoEncontradoException.class)
    public ResponseEntity<Void> tratarCartaoNaoEncontrado() {
        return ResponseEntity.notFound().build();
    }

    /**
     * Transação barrada por uma regra: 422 com o motivo em texto puro (SALDO_INSUFICIENTE, SENHA_INVALIDA
     * ou CARTAO_INEXISTENTE).
     */
    @ExceptionHandler(TransacaoNaoAutorizadaException.class)
    public ResponseEntity<String> tratarTransacaoNaoAutorizada(final TransacaoNaoAutorizadaException excecao) {
        return respostaTexto(HttpStatus.UNPROCESSABLE_ENTITY, excecao.getMotivo().name());
    }

    /**
     * Conflito de concorrência persistente no mesmo cartão: 409, para o cliente decidir se tenta de novo.
     */
    @ExceptionHandler(ConcorrenciaTransacaoException.class)
    public ResponseEntity<String> tratarConcorrencia() {
        return respostaTexto(HttpStatus.CONFLICT, CORPO_CONFLITO_CONCORRENCIA);
    }

    /**
     * Falha de Bean Validation: 400 listando campo e mensagem de cada violação.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroValidacaoResponse> tratarValidacao(final MethodArgumentNotValidException excecao) {
        final List<ErroValidacaoResponse.CampoInvalido> erros = excecao.getBindingResult().getFieldErrors().stream()
                .map(erro -> new ErroValidacaoResponse.CampoInvalido(erro.getField(), erro.getDefaultMessage()))
                .toList();
        return respostaValidacao(erros);
    }

    /**
     * Corpo ausente ou JSON que não pôde ser lido: 400 no mesmo formato dos erros de validação.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroValidacaoResponse> tratarCorpoIlegivel() {
        return respostaValidacao(
                List.of(new ErroValidacaoResponse.CampoInvalido(CAMPO_CORPO_REQUISICAO, MENSAGEM_CORPO_ILEGIVEL)));
    }

    private ResponseEntity<ErroValidacaoResponse> respostaValidacao(final List<ErroValidacaoResponse.CampoInvalido> erros) {
        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErroValidacaoResponse(erros));
    }

    private ResponseEntity<String> respostaTexto(final HttpStatus status, final String corpo) {
        return ResponseEntity.status(status).contentType(MediaType.TEXT_PLAIN).body(corpo);
    }
}
