package br.com.vr.miniautorizador.cartao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;

/**
 * Cartão de benefício persistido. Concentra as regras de saldo (consulta e débito) e usa
 * lock otimista ({@code @Version}) para impedir que transações concorrentes sobrescrevam o saldo umas das outras.
 * A senha é guardada somente como hash.
 */
@Entity
@Table(name = "cartao")
public class Cartao {

    private static final int PRECISAO_SALDO = 15;
    private static final int ESCALA_SALDO = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_cartao", nullable = false, unique = true, length = 19)
    private String numeroCartao;

    @Column(name = "senha", nullable = false, length = 100)
    private String senha;

    @Column(name = "saldo", nullable = false, precision = PRECISAO_SALDO, scale = ESCALA_SALDO)
    private BigDecimal saldo;

    @Version
    @Column(name = "versao", nullable = false)
    private Long versao;

    protected Cartao() {
        // Exigido pelo JPA.
    }

    private Cartao(final String numeroCartao, final String senhaCriptografada, final BigDecimal saldoInicial) {
        this.numeroCartao = numeroCartao;
        this.senha = senhaCriptografada;
        this.saldo = saldoInicial;
    }

    /**
     * Cria um cartão novo.
     *
     * @param numeroCartao        número do cartão
     * @param senhaCriptografada  hash da senha (nunca a senha em claro)
     * @param saldoInicial        saldo com que o cartão nasce
     * @return cartão ainda não persistido
     */
    public static Cartao criar(final String numeroCartao, final String senhaCriptografada,
                               final BigDecimal saldoInicial) {
        return new Cartao(numeroCartao, senhaCriptografada, saldoInicial);
    }

    /**
     * Indica se o saldo cobre o valor informado.
     */
    public boolean possuiSaldoPara(final BigDecimal valor) {
        return saldo.compareTo(valor) >= 0;
    }

    /**
     * Subtrai o valor do saldo. A verificação de saldo suficiente é responsabilidade das regras de autorização,
     * executadas antes do débito.
     */
    public void debitar(final BigDecimal valor) {
        this.saldo = saldo.subtract(valor);
    }

    public Long getId() {
        return id;
    }

    public String getNumeroCartao() {
        return numeroCartao;
    }

    public String getSenha() {
        return senha;
    }

    public BigDecimal getSaldo() {
        return saldo;
    }

    public Long getVersao() {
        return versao;
    }
}
