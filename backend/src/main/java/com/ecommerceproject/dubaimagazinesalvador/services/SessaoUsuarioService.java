package com.ecommerceproject.dubaimagazinesalvador.services;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerceproject.dubaimagazinesalvador.repositories.UsuarioRepository;

@Service
public class SessaoUsuarioService {

    private final UsuarioRepository usuarioRepository;

    public SessaoUsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public void revogarTodas(UUID usuarioId) {
        usuarioRepository.buscarPorIdParaAtualizacao(usuarioId).ifPresent(usuario -> {
            usuario.invalidarTokensEmitidos();
            usuarioRepository.save(usuario);
        });
    }
}
