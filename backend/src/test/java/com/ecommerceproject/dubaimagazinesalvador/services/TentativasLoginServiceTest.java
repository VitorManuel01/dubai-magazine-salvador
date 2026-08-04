package com.ecommerceproject.dubaimagazinesalvador.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ecommerceproject.dubaimagazinesalvador.domain.usuarios.Funcionario;
import com.ecommerceproject.dubaimagazinesalvador.repositories.UsuarioRepository;

class TentativasLoginServiceTest {

    private UsuarioRepository repository;
    private TentativasLoginService service;
    private Funcionario usuario;

    @BeforeEach
    void configurar() {
        repository = mock(UsuarioRepository.class);
        service = new TentativasLoginService(repository, 3, Duration.ofMinutes(20));
        usuario = new Funcionario();
        usuario.setId(UUID.randomUUID());
        usuario.setCodigoSantri("FUN-001");
        when(repository.buscarPorCodigoSantriParaAtualizacao("FUN-001"))
                .thenReturn(Optional.of(usuario));
        when(repository.findById(usuario.getId())).thenReturn(Optional.of(usuario));
    }

    @Test
    void deveBloquearContaNaTerceiraFalhaPorVinteMinutos() {
        assertFalse(service.registrarFalha("fun-001").bloqueado());
        assertFalse(service.registrarFalha("FUN-001").bloqueado());
        Instant antesDaTerceira = Instant.now();

        var bloqueio = service.registrarFalha("FUN-001");

        assertTrue(bloqueio.bloqueado());
        assertEquals(3, usuario.getTentativasLoginFalhas());
        assertNotNull(usuario.getBloqueadoAte());
        assertTrue(usuario.getBloqueadoAte().isAfter(
                antesDaTerceira.plus(Duration.ofMinutes(19))
        ));
        assertTrue(usuario.getBloqueadoAte().isBefore(
                antesDaTerceira.plus(Duration.ofMinutes(21))
        ));
    }

    @Test
    void deveLimparFalhasDepoisDeLoginBemSucedido() {
        usuario.setTentativasLoginFalhas(2);
        usuario.setBloqueadoAte(Instant.now().minusSeconds(1));

        service.registrarSucesso(usuario.getId());

        assertEquals(0, usuario.getTentativasLoginFalhas());
        assertNull(usuario.getBloqueadoAte());
    }
}
