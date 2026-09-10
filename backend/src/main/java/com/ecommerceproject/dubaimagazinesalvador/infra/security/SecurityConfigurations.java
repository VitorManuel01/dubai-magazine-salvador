package com.ecommerceproject.dubaimagazinesalvador.infra.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfigurations {

    private final SecurityFilter securityFilter;
    private final HttpsObrigatorioFilter httpsObrigatorioFilter;

    public SecurityConfigurations(
            SecurityFilter securityFilter,
            HttpsObrigatorioFilter httpsObrigatorioFilter
    ) {
        this.securityFilter = securityFilter;
        this.httpsObrigatorioFilter = httpsObrigatorioFilter;
    }

    @Bean
    public FilterRegistrationBean<SecurityFilter> registroServletSecurityFilter() {
        FilterRegistrationBean<SecurityFilter> registro = new FilterRegistrationBean<>(securityFilter);
        registro.setEnabled(false);
        return registro;
    }

    @Bean
    public FilterRegistrationBean<HttpsObrigatorioFilter> registroServletHttpsFilter() {
        FilterRegistrationBean<HttpsObrigatorioFilter> registro =
                new FilterRegistrationBean<>(httpsObrigatorioFilter);
        registro.setEnabled(false);
        return registro;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity httpSecurity,
            CorsConfigurationSource corsConfigurationSource
    ) throws Exception {
        httpSecurity
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) ->
                                responderErro(response, 401, "Autenticação necessária."))
                        .accessDeniedHandler((request, response, exception) ->
                                responderErro(response, 403, "Acesso negado."))
                )
                .authorizeHttpRequests(authorize -> authorize
                        // Só o despacho interno de erro pode renderizar /error sem nova autenticação.
                        // Uma chamada HTTP direta a /error continua protegida por anyRequest().
                        .requestMatchers(request -> request.getDispatcherType() == DispatcherType.ERROR
                                && "/error".equals(request.getServletPath())).permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/registerADM").hasRole("ADMIN")
                        .requestMatchers("/cliente", "/cliente/**").denyAll()
                        .requestMatchers("/funcionario", "/funcionario/**").hasRole("ADMIN")
                        .requestMatchers("/actuator", "/actuator/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/admin/importacoes/produtos").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/admin/importacoes/promocoes").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/admin/importacoes/estoque").hasRole("ADMIN")
                        .requestMatchers("/admin/vitrines-home/**").hasRole("ADMIN")
                        .requestMatchers("/admin/banners-home/**").hasRole("ADMIN")
                        .requestMatchers(
                                "/admin/depoimentos-home",
                                "/admin/depoimentos-home/**"
                        ).hasRole("ADMIN")
                        .requestMatchers("/admin/produtos", "/admin/produtos/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/interno/produtos")
                                .hasAnyRole("ADMIN", "FUNCIONARIO")
                        .requestMatchers(HttpMethod.GET, "/admin/categorias").hasRole("ADMIN")
                        .requestMatchers("/admin/vitrine-loja", "/admin/vitrine-loja/**")
                                .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/vitrine-loja", "/vitrine-loja/**")
                                .hasAnyRole("ADMIN", "FUNCIONARIO")
                        .requestMatchers(HttpMethod.POST, "/produto").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/produto/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/produto/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/produto", "/produto/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/categoria").permitAll()
                        .requestMatchers(HttpMethod.GET, "/vitrines-home").permitAll()
                        .requestMatchers(HttpMethod.GET, "/banners-home").permitAll()
                        .requestMatchers(HttpMethod.GET, "/depoimentos-home").permitAll()
                        .requestMatchers(HttpMethod.GET, "/catalogo/imagens/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'none'; frame-ancestors 'none'; base-uri 'none'"
                        ))
                        .referrerPolicy(referrer -> referrer.policy(
                                ReferrerPolicyHeaderWriter.ReferrerPolicy
                                        .STRICT_ORIGIN_WHEN_CROSS_ORIGIN
                        ))
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .preload(true)
                                .maxAgeInSeconds(31_536_000)
                        )
                        .permissionsPolicy(permissions -> permissions.policy(
                                "camera=(), microphone=(), geolocation=(), payment=(), usb=()"
                        ))
                )
                .addFilterBefore(httpsObrigatorioFilter, SecurityContextHolderFilter.class)
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class);

        return httpSecurity.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:http://localhost:5173}") String origensPermitidas
    ) {
        List<String> origens = Arrays.stream(origensPermitidas.split(","))
                .map(String::trim)
                .filter(origem -> !origem.isBlank())
                .toList();

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(origens);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "X-Device-Id"
        ));
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private void responderErro(
            HttpServletResponse response,
            int status,
            String mensagem
    ) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"erro\":\"" + mensagem + "\"}");
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

}
