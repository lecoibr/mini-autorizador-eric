package br.com.vr.miniautorizador.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Metadados da documentação OpenAPI e declaração do esquema HTTP Basic exigido pela API.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Mini Autorizador",
                version = "1.0.0",
                description = "API de criação de cartões de benefício, consulta de saldo e autorização de transações."),
        security = @SecurityRequirement(name = OpenApiConfig.ESQUEMA_BASIC))
@SecurityScheme(
        name = OpenApiConfig.ESQUEMA_BASIC,
        type = SecuritySchemeType.HTTP,
        scheme = "basic")
public class OpenApiConfig {

    static final String ESQUEMA_BASIC = "basicAuth";
}
