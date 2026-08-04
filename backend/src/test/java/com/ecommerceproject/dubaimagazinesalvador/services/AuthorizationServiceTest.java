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
import com.ecommerceproject.dubaimagazinesalvador.repositories.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private AuthorizationService authorizationService;

    @Test
    void clienteNaoPodeMaisAutenticar() {
        Cliente cliente = new Cliente();
        cliente.setFuncao(Role.ROLE_CLIENTE);
        when(usuarioRepository.findFirstByCodigoSantriIgnoreCase("CLI-001"))
                .thenReturn(Optional.of(cliente));

        assertThrows(
                UsernameNotFoundException.class,
                () -> authorizationService.loadUserByUsername("cli-001")
        );
    }

    @Test
    void funcionarioAutenticaSomentePeloCodigoSantri() {
        Funcionario funcionario = new Funcionario();
        funcionario.setFuncao(Role.ROLE_FUNCIONARIO);
        when(usuarioRepository.findFirstByCodigoSantriIgnoreCase("FUN-001"))
                .thenReturn(Optional.of(funcionario));

        assertSame(
                funcionario,
                authorizationService.loadUserByUsername("fun-001")
        );
    }
}
