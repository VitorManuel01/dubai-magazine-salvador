package com.ecommerceproject.dubaimagazinesalvador.services;

import java.util.Locale;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.ecommerceproject.dubaimagazinesalvador.domain.usuarios.Role;
import com.ecommerceproject.dubaimagazinesalvador.repositories.UsuarioRepository;

@Service
public class AuthorizationService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public AuthorizationService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String codigoSantri) {
        String codigoNormalizado = codigoSantri.trim().toUpperCase(Locale.ROOT);
        return usuarioRepository.findFirstByCodigoSantriIgnoreCase(codigoNormalizado)
                .filter(usuario -> usuario.getFuncao() == Role.ROLE_ADMIN
                        || usuario.getFuncao() == Role.ROLE_FUNCIONARIO)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Credenciais inválidas."
                ));
    }
}
