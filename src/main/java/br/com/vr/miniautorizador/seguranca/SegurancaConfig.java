package br.com.vr.miniautorizador.seguranca;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Segurança da API: HTTP Basic stateless, com exceção da documentação Swagger e do health check.
 * O mesmo {@link PasswordEncoder} BCrypt protege a senha do usuário da API e as senhas dos cartões.
 */
@Configuration
public class SegurancaConfig {

    private static final String[] ROTAS_PUBLICAS = {
        "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/actuator/health"
    };

    // Destino do dispatch interno de erro do container (400 de JSON ilegível, 404, 405...). Sem liberá-lo,
    // o dispatch passaria de novo pela autorização sem credenciais e o cliente receberia 401 em vez do erro real.
    private static final String ROTA_ERRO = "/error";

    private static final String PAPEL_USUARIO = "USER";

    /**
     * Cadeia de filtros da API: HTTP Basic sem sessão; documentação e health públicos; todo o resto autenticado.
     * A assinatura {@code throws Exception} é imposta por {@link HttpSecurity#build()}.
     */
    @Bean
    public SecurityFilterChain filtroDeSeguranca(final HttpSecurity http) throws Exception {
        // CSRF desabilitado: API stateless autenticada por header Authorization (Basic), consumida por máquinas,
        // sem cookie de sessão nem navegador; o ataque CSRF depende de credencial anexada automaticamente pelo browser.
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(rotas -> rotas
                        .requestMatchers(ROTA_ERRO).permitAll()
                        .requestMatchers(HttpMethod.GET, ROTAS_PUBLICAS).permitAll()
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .build();
    }

    /**
     * Codificador único para a senha do usuário da API e para a senha dos cartões.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Usuário único da API, montado em memória a partir das propriedades configuradas.
     */
    @Bean
    public UserDetailsService userDetailsService(final SegurancaProperties properties,
                                                 final PasswordEncoder passwordEncoder) {
        final UserDetails usuario = User.withUsername(properties.usuario())
                .password(passwordEncoder.encode(properties.senha()))
                .roles(PAPEL_USUARIO)
                .build();
        return new InMemoryUserDetailsManager(usuario);
    }
}
