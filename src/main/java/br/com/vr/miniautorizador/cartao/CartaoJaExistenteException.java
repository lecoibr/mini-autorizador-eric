package br.com.vr.miniautorizador.cartao;

/**
 * Lançada ao tentar criar um cartão cujo número já existe. Carrega o request original porque o contrato
 * da API devolve esses mesmos dados no corpo da resposta 422.
 */
public class CartaoJaExistenteException extends RuntimeException {

    private final transient CartaoRequest request;

    public CartaoJaExistenteException(final CartaoRequest request) {
        this(request, null);
    }

    public CartaoJaExistenteException(final CartaoRequest request, final Throwable causa) {
        super("Cartão já existente: " + request.numeroCartao(), causa);
        this.request = request;
    }

    public CartaoRequest getRequest() {
        return request;
    }
}
