package com.ecommerceproject.dubaimagazinesalvador.infra.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.ecommerceproject.dubaimagazinesalvador.domain.usuarios.Usuario;
import com.ecommerceproject.dubaimagazinesalvador.services.AuditoriaAdministrativaService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AuditoriaAdministrativaInterceptor implements HandlerInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(
            AuditoriaAdministrativaInterceptor.class
    );
    private static final String CONTEXTO = AuditoriaAdministrativaInterceptor.class.getName();

    private final AuditoriaAdministrativaService auditoria;

    public AuditoriaAdministrativaInterceptor(AuditoriaAdministrativaService auditoria) {
        this.auditoria = auditoria;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) {
        if (ehMetodoSomenteLeitura(request.getMethod())) {
            return true;
        }
        var autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao != null
                && autenticacao.isAuthenticated()
                && autenticacao.getPrincipal() instanceof Usuario usuario) {
            request.setAttribute(CONTEXTO, new ContextoAuditoria(
                    usuario,
                    request.getRemoteAddr(),
                    request.getHeader("User-Agent")
            ));
        }
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception exception
    ) {
        Object contexto = request.getAttribute(CONTEXTO);
        if (!(contexto instanceof ContextoAuditoria dados)) {
            return;
        }
        try {
            auditoria.registrar(
                    dados.usuario().getId(),
                    dados.usuario().getCodigoSantri(),
                    dados.usuario().getFuncao(),
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    dados.enderecoIp(),
                    dados.userAgent()
            );
        } catch (RuntimeException e) {
            LOG.error("Não foi possível persistir a trilha de auditoria administrativa.", e);
        }
    }

    private boolean ehMetodoSomenteLeitura(String metodo) {
        return "GET".equalsIgnoreCase(metodo)
                || "HEAD".equalsIgnoreCase(metodo)
                || "OPTIONS".equalsIgnoreCase(metodo);
    }

    private record ContextoAuditoria(Usuario usuario, String enderecoIp, String userAgent) {
    }
}
