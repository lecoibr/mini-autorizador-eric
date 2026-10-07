package br.com.vr.miniautorizador.transacao;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint REST de autorização de transações.
 */
@RestController
@RequestMapping("/transacoes")
@Tag(name = "Transações", description = "Autorização de transações com cartão")
public class TransacaoController {

    private static final String RESPOSTA_AUTORIZADA = "OK";

    private final TransacaoService service;

    public TransacaoController(final TransacaoService service) {
        this.service = service;
    }

    /**
     * Autoriza a transação: aplica as regras e, se todas passarem, debita o valor do saldo do cartão.
     * O Content-Type é fixado na resposta, e não em "produces", para que o endpoint atenda qualquer header
     * Accept enviado pela maquininha/cliente sem responder 406.
     */
    @PostMapping
    @Operation(summary = "Autoriza uma transação e debita o saldo do cartão")
    @ApiResponse(responseCode = "201", description = "Transação autorizada (corpo: OK)")
    @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos", content = @Content)
    @ApiResponse(responseCode = "401", description = "Erro de autenticação", content = @Content)
    @ApiResponse(responseCode = "409", description = "CONFLITO_CONCORRENCIA: contenção extrema no mesmo cartão")
    @ApiResponse(responseCode = "422",
            description = "Não autorizada: SALDO_INSUFICIENTE, SENHA_INVALIDA ou CARTAO_INEXISTENTE")
    public ResponseEntity<String> autorizar(@Valid @RequestBody final TransacaoRequest request) {
        service.autorizar(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .contentType(MediaType.TEXT_PLAIN)
                .body(RESPOSTA_AUTORIZADA);
    }
}
