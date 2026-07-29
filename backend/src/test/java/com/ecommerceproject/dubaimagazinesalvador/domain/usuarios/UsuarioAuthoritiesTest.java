package com.ecommerceproject.dubaimagazinesalvador.domain.usuarios;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

class UsuarioAuthoritiesTest {

    @Test
    void deveConcederSomenteAPermissaoDoUsuario() {
        Cliente cliente = new Cliente();
        cliente.setFuncao(Role.ROLE_CLIENTE);

        List<String> permissoes = cliente.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        assertEquals(List.of("ROLE_CLIENTE"), permissoes);
    }
}
