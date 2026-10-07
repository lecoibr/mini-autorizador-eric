package br.com.vr.miniautorizador.comum;

import java.util.List;

/**
 * Corpo da resposta 400 para falhas de Bean Validation.
 *
 * @param erros lista de campos inválidos com a respectiva mensagem
 */
public record ErroValidacaoResponse(List<CampoInvalido> erros) {

    /**
     * Um campo inválido e o motivo.
     */
    public record CampoInvalido(String campo, String mensagem) {
    }
}
