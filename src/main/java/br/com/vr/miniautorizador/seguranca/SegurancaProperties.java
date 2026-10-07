package br.com.vr.miniautorizador.seguranca;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Credenciais do usuário da API (HTTP Basic), externalizadas no application.yml e validadas na subida,
 * para que a aplicação não inicie sem um usuário utilizável.
 *
 * @param usuario login aceito pela API
 * @param senha   senha em claro; é codificada com BCrypt ao montar o usuário em memória
 */
@Validated
@ConfigurationProperties(prefix = "mini-autorizador.seguranca")
public record SegurancaProperties(@NotBlank String usuario, @NotBlank String senha) {
}
