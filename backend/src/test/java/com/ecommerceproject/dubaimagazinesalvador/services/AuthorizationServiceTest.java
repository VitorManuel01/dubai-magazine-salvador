package com.ecommerceproject.dubaimagazinesalvador.services;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.ecommerceproject.dubaimagazinesalvador.domain.usuarios.Cliente;
import com.ecommerceproject.dubaimagazinesalvador.domain.usuarios.Funcionario;
import com.ecommerceproject.dubaimagazinesalvador.domain.usuarios.Role;
import com.ecommerceproject.dubaimagazinesalvador.repositories.AdministradorRespository;
import com.ecommerceproject.dubaimagazinesalvador.repositories.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AdministradorRespository administradorRespository;

    @InjectMocks
    private AuthorizationService authorizationService;

    @Test
    void clienteNaoPodeMaisAutenticar() {
        Cliente cliente = new Cliente();
        cliente.setFuncao(Role.ROLE_CLIENTE);

        when(administradorRespository.findByLogin("cliente"))
                .thenReturn(null);
        when(administradorRespository.findByEmail("cliente"))
                .thenReturn(null);
        when(usuarioRepository.findFirstByEmailIgnoreCase("cliente"))
                .thenReturn(Optional.of(cliente));

        assertThrows(
                UsernameNotFoundException.class,
                () -> authorizationService.loadUserByUsername("cliente")
        );
    }

    @Test
    void funcionarioContinuaAutenticando() {
        Funcionario funcionario = new Funcionario();
        funcionario.setFuncao(Role.ROLE_FUNCIONARIO);

        when(administradorRespository.findByLogin("funcionario"))
                .thenReturn(null);
        when(administradorRespository.findByEmail("funcionario"))
                .thenReturn(null);
        when(usuarioRepository.findFirstByEmailIgnoreCase("funcionario"))
                .thenReturn(Optional.of(funcionario));

        assertSame(
                funcionario,
                authorizationService.loadUserByUsername("funcionario")
        );
    }
}
