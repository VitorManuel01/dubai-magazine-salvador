package com.ecommerceproject.dubaimagazinesalvador.infra.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Impede acesso HTTP nos ambientes publicados.
 *
 * O Tomcat normaliza X-Forwarded-* somente para conexões do proxy loopback.
 * Assim, request.isSecure() representa o protocolo usado pelo cliente, mesmo
 * quando o TLS termina no Cloudflare ou no proxy da hospedagem.
 */
@Component
public class HttpsObrigatorioFilter extends OncePerRequestFilter {

    private final boolean httpsObrigatorio;

    public HttpsObrigatorioFilter(
            @Value("${app.security.require-https:false}") boolean httpsObrigatorio
    ) {
        this.httpsObrigatorio = httpsObrigatorio;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (httpsObrigatorio && !request.isSecure()) {
            response.setStatus(HttpStatus.UPGRADE_REQUIRED.value());
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(
                    "{\"erro\":\"HTTPS é obrigatório neste ambiente.\"}"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }
}
