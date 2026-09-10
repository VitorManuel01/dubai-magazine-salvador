package com.ecommerceproject.dubaimagazinesalvador.infra.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.http.MediaType;

import com.ecommerceproject.dubaimagazinesalvador.domain.usuarios.Usuario;
import com.ecommerceproject.dubaimagazinesalvador.repositories.UsuarioRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class SecurityFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final UsuarioRepository usuarioRepository;

    public SecurityFilter(TokenService tokenService, UsuarioRepository usuarioRepository) {
        this.tokenService = tokenService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (request.getRequestURI().equals("/auth/login")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = recoverToken(request);
        if (token != null) {
            TokenService.TokenValidado tokenValidado = tokenService.validateTokenCompleto(token);
            if (tokenValidado == null) {
                responderNaoAutorizado(response);
                return;
            }

            Usuario usuario = usuarioRepository.findById(tokenValidado.usuarioId()).orElse(null);
            if (usuario == null
                    || !usuario.isEnabled()
                    || usuario.getVersaoToken() != tokenValidado.versao()) {
                responderNaoAutorizado(response);
                return;
            }

            var authentication = new UsernamePasswordAuthenticationToken(
                    usuario,
                    null,
                    usuario.getAuthorities()
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private String recoverToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.substring(7).trim();
        return token.isEmpty() ? null : token;
    }

    private void responderNaoAutorizado(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"erro\":\"Sessão inválida ou expirada.\"}");
    }
}
