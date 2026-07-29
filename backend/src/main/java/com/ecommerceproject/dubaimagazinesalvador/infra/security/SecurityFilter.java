package com.ecommerceproject.dubaimagazinesalvador.infra.security;

import java.io.IOException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.ecommerceproject.dubaimagazinesalvador.domain.usuarios.Usuario;
import com.ecommerceproject.dubaimagazinesalvador.repositories.UsuarioRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class SecurityFilter extends OncePerRequestFilter {

    @Autowired
    TokenService tokenService;
    @Autowired
    UsuarioRepository usuarioRepository;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        var token = this.recoverToken(request);

        if (request.getRequestURI().equals("/auth/login")) {
            filterChain.doFilter(request, response);
            return;
        }

        if (token != null) {
            var login = tokenService.validateToken(token);

            if (login != null) {
                Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(login);

                if (usuarioOpt.isEmpty()) {
                    usuarioOpt = usuarioRepository.findByLogin(login);
                }

                if (usuarioOpt.isPresent()) {
                    Usuario usuario = usuarioOpt.get();
                    var authentication = new UsernamePasswordAuthenticationToken(
                            usuario,
                            null,
                            usuario.getAuthorities()
                    );
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Usuário não encontrado");
                    return;
                }
            } else {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token inválido ou expirado");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
    
    private String recoverToken(HttpServletRequest request){
        var authHeader = request.getHeader("Authorization");
        if(authHeader == null) return null;
        return authHeader.replace("Bearer", "");

    }
}
