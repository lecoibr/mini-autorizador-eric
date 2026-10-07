package br.com.vr.miniautorizador.cartao;

/**
 * Lançada ao consultar um cartão que não existe.
 */
public class CartaoNaoEncontradoException extends RuntimeException {

    public CartaoNaoEncontradoException(final String numeroCartao) {
        super("Cartão não encontrado: " + numeroCartao);
    }
}
