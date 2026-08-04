package com.ecommerceproject.dubaimagazinesalvador.infra.security;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfigurations {

    private final SecurityFilter securityFilter;

    public SecurityConfigurations(SecurityFilter securityFilter) {
        this.securityFilter = securityFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity httpSecurity,
            CorsConfigurationSource corsConfigurationSource,
            @Value("${app.security.require-https:false}") boolean requireHttps
    ) throws Exception {
        httpSecurity
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/registerADM").hasRole("ADMIN")
                        .requestMatchers("/cliente", "/cliente/**").denyAll()
                        .requestMatchers("/funcionario", "/funcionario/**").hasRole("ADMIN")
                        .requestMatchers("/actuator", "/actuator/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/admin/importacoes/produtos").hasRole("ADMIN")
                        .requestMatchers("/admin/vitrines-home/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/admin/produtos").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/admin/categorias").hasRole("ADMIN")
                        .requestMatchers("/admin/vitrine-loja", "/admin/vitrine-loja/**")
                                .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/vitrine-loja", "/vitrine-loja/**")
                                .hasAnyRole("ADMIN", "FUNCIONARIO")
                        .requestMatchers(HttpMethod.POST, "/produto").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/produto/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/produto/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/produto").permitAll()
                        .requestMatchers(HttpMethod.GET, "/categoria").permitAll()
                        .requestMatchers(HttpMethod.GET, "/vitrines-home").permitAll()
                        .requestMatchers(HttpMethod.GET, "/catalogo/imagens/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/uploads/produtos/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .headers(headers -> headers
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .preload(true)
                                .maxAgeInSeconds(31_536_000)
                        )
                )
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class);

        if (requireHttps) {
            httpSecurity.requiresChannel(channel -> channel.anyRequest().requiresSecure());
        }

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
