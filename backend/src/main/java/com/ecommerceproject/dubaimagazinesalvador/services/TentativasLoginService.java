package com.ecommerceproject.dubaimagazinesalvador.services;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerceproject.dubaimagazinesalvador.domain.usuarios.Usuario;
import com.ecommerceproject.dubaimagazinesalvador.repositories.UsuarioRepository;

@Service
public class TentativasLoginService {

    private final UsuarioRepository usuarioRepository;
    private final int maximoTentativas;
    private final Duration duracaoBloqueio;

    public TentativasLoginService(
            UsuarioRepository usuarioRepository,
            @Value("${app.security.login.max-failed-attempts:3}") int maximoTentativas,
            @Value("${app.security.login.lock-duration:PT20M}") Duration duracaoBloqueio
    ) {
        this.usuarioRepository = usuarioRepository;
        this.maximoTentativas = maximoTentativas;
        this.duracaoBloqueio = duracaoBloqueio;
    }

    @Transactional
    public EstadoBloqueio registrarFalha(String codigoSantri) {
        Optional<Usuario> usuario = buscarContaParaAtualizacao(codigoSantri);
        if (usuario.isEmpty()) {
            return EstadoBloqueio.liberado();
        }

        Usuario conta = usuario.get();
        Instant agora = Instant.now();
        if (conta.getBloqueadoAte() != null && agora.isBefore(conta.getBloqueadoAte())) {
            return EstadoBloqueio.bloqueado(conta.getBloqueadoAte());
        }
        if (conta.getBloqueadoAte() != null) {
            limpar(conta);
        }

        int tentativas = conta.getTentativasLoginFalhas() + 1;
        conta.setTentativasLoginFalhas(tentativas);
        if (tentativas >= maximoTentativas) {
            Instant bloqueadoAte = agora.plus(duracaoBloqueio);
            conta.setBloqueadoAte(bloqueadoAte);
            usuarioRepository.save(conta);
            return EstadoBloqueio.bloqueado(bloqueadoAte);
        }

        usuarioRepository.save(conta);
        return EstadoBloqueio.liberado();
    }

    @Transactional
    public void registrarSucesso(UUID usuarioId) {
        usuarioRepository.findById(usuarioId).ifPresent(conta -> {
            if (conta.getTentativasLoginFalhas() != 0 || conta.getBloqueadoAte() != null) {
                limpar(conta);
                usuarioRepository.save(conta);
            }
        });
    }

    private Optional<Usuario> buscarContaParaAtualizacao(String codigoSantri) {
        String normalizado = codigoSantri == null
                ? ""
                : codigoSantri.trim().toUpperCase(Locale.ROOT);
        return usuarioRepository.buscarPorCodigoSantriParaAtualizacao(normalizado);
    }

    private void limpar(Usuario conta) {
        conta.setTentativasLoginFalhas(0);
        conta.setBloqueadoAte(null);
    }

    public record EstadoBloqueio(boolean bloqueado, Instant bloqueadoAte) {

        private static EstadoBloqueio liberado() {
            return new EstadoBloqueio(false, null);
        }

        private static EstadoBloqueio bloqueado(Instant bloqueadoAte) {
            return new EstadoBloqueio(true, bloqueadoAte);
        }
    }
}
