package br.com.vr.miniautorizador.cartao;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints REST de criação de cartão e consulta de saldo.
 */
@RestController
@RequestMapping("/cartoes")
@Tag(name = "Cartões", description = "Criação de cartões e consulta de saldo")
public class CartaoController {

    private final CartaoService service;

    public CartaoController(final CartaoService service) {
        this.service = service;
    }

    /**
     * Cria um cartão com o saldo inicial configurado e devolve os dados recebidos.
     */
    @PostMapping
    @Operation(summary = "Cria um novo cartão com o saldo inicial configurado")
    @ApiResponse(responseCode = "201", description = "Cartão criado")
    @ApiResponse(responseCode = "400", description = "Número do cartão ou senha ausentes")
    @ApiResponse(responseCode = "401", description = "Erro de autenticação", content = @Content)
    @ApiResponse(responseCode = "422", description = "Cartão já existente (devolve os mesmos dados da requisição)")
    public ResponseEntity<CartaoResponse> criar(@Valid @RequestBody final CartaoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(request));
    }

    /**
     * Devolve o saldo atual do cartão como número puro no corpo (ex.: {@code 495.15}).
     */
    @GetMapping("/{numeroCartao}")
    @Operation(summary = "Obtém o saldo do cartão")
    @ApiResponse(responseCode = "200", description = "Saldo do cartão")
    @ApiResponse(responseCode = "401", description = "Erro de autenticação", content = @Content)
    @ApiResponse(responseCode = "404", description = "Cartão inexistente", content = @Content)
    public ResponseEntity<BigDecimal> consultarSaldo(@PathVariable("numeroCartao") final String numeroCartao) {
        return ResponseEntity.ok(service.consultarSaldo(numeroCartao));
    }
}
