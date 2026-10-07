package br.com.vr.miniautorizador.cartao;

/**
 * Resposta de criação de cartão. O contrato exige ecoar exatamente os dados recebidos na requisição,
 * por isso a senha vem do request e não do banco (onde só existe o hash).
 *
 * @param senha        senha recebida na requisição
 * @param numeroCartao número do cartão recebido na requisição
 */
public record CartaoResponse(String senha, String numeroCartao) {

    /**
     * Monta a resposta a partir do request original.
     */
    public static CartaoResponse de(final CartaoRequest request) {
        return new CartaoResponse(request.senha(), request.numeroCartao());
    }
}
