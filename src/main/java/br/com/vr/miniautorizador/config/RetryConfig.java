package br.com.vr.miniautorizador.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Habilita o suporte a {@code @Retryable}, usado para repetir transações em conflito de lock otimista.
 */
@Configuration
@EnableRetry
public class RetryConfig {
}
