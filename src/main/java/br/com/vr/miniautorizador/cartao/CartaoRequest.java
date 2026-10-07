package br.com.vr.miniautorizador.cartao;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Corpo da requisição de criação de cartão.
 *
 * <p>Os limites de tamanho acompanham a coluna {@code numero_cartao VARCHAR(19)} e o máximo de 72 bytes que o
 * BCrypt considera na senha; sem eles, uma entrada grande demais estouraria no banco ou no codificador.</p>
 *
 * @param numeroCartao número do cartão a ser criado
 * @param senha        senha em claro informada pelo cliente (só trafega na requisição; é persistida como hash)
 */
public record CartaoRequest(
        @NotBlank @Size(max = TAMANHO_MAXIMO_NUMERO_CARTAO) String numeroCartao,
        @NotBlank @Size(max = TAMANHO_MAXIMO_SENHA) String senha) {

    public static final int TAMANHO_MAXIMO_NUMERO_CARTAO = 19;
    public static final int TAMANHO_MAXIMO_SENHA = 72;
}
