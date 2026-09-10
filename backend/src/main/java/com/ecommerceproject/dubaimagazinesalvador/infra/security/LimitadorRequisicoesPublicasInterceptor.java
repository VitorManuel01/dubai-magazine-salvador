package com.ecommerceproject.dubaimagazinesalvador.infra.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Semaphore;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.ecommerceproject.dubaimagazinesalvador.services.LimitadorRequisicoesPublicasService;
import com.ecommerceproject.dubaimagazinesalvador.services.LimitadorRequisicoesPublicasService.TipoRequisicao;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class LimitadorRequisicoesPublicasInterceptor implements HandlerInterceptor {

    private static final byte[] RESPOSTA_LIMITE =
            "{\"erro\":\"Muitas requisições. Aguarde antes de tentar novamente.\"}"
                    .getBytes(StandardCharsets.UTF_8);

    private final LimitadorRequisicoesPublicasService limitador;
    private final Semaphore consultasSimultaneas;
    private static final String VAGA = LimitadorRequisicoesPublicasInterceptor.class.getName();

    public LimitadorRequisicoesPublicasInterceptor(
            LimitadorRequisicoesPublicasService limitador,
            @Value("${app.security.public-rate-limit.catalog.max-concurrent:4}") int maximoSimultaneas
    ) {
        if (maximoSimultaneas < 1 || maximoSimultaneas > 32) {
            throw new IllegalArgumentException("O limite de consultas simultâneas deve estar entre 1 e 32.");
        }
        this.limitador = limitador;
        this.consultasSimultaneas = new Semaphore(maximoSimultaneas);
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) throws IOException {
        if (!"GET".equalsIgnoreCase(request.getMethod())
                && !"HEAD".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        TipoRequisicao tipo = request.getRequestURI().startsWith("/catalogo/imagens/")
                ? TipoRequisicao.IMAGEM
                : TipoRequisicao.CATALOGO;
        var resultado = limitador.consumir(request.getRemoteAddr(), tipo);
        if (resultado.permitido()) {
            if (tipo == TipoRequisicao.CATALOGO) {
                // Não acumula uma fila de buscas que ocuparia threads/conexões do servidor.
                if (!consultasSimultaneas.tryAcquire()) {
                    response.setStatus(503);
                    response.setHeader(HttpHeaders.RETRY_AFTER, "1");
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                    response.getWriter().write("{\"erro\":\"Catálogo ocupado. Tente novamente em instantes.\"}");
                    return false;
                }
                request.setAttribute(VAGA, Boolean.TRUE);
            }
            return true;
        }

        response.setStatus(429);
        response.setHeader(
                HttpHeaders.RETRY_AFTER,
                Long.toString(resultado.tentarNovamenteEmSegundos())
        );
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getOutputStream().write(RESPOSTA_LIMITE);
        return false;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
            Object handler, Exception exception) {
        if (Boolean.TRUE.equals(request.getAttribute(VAGA))) {
            request.removeAttribute(VAGA);
            consultasSimultaneas.release();
        }
    }
}
