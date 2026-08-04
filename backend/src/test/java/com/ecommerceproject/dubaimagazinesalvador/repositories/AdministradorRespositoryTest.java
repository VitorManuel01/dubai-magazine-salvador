package com.ecommerceproject.dubaimagazinesalvador.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerceproject.dubaimagazinesalvador.domain.usuarios.Administrador;
import com.ecommerceproject.dubaimagazinesalvador.domain.usuarios.Role;
import com.ecommerceproject.dubaimagazinesalvador.infra.security.SecurityFilter;
import com.ecommerceproject.dubaimagazinesalvador.infra.security.TokenService;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AdministradorRespositoryTest {

    @Autowired
    private AdministradorRespository administradorRespository;

    @MockBean
    private TokenService tokenService;

    @MockBean
    private SecurityFilter securityFilter;

    @Test
    @Transactional
    @DisplayName("Deve retornar administrador pelo código Santri")
    void findByCodigoSantriCase1() {
        Administrador administrador = new Administrador();
        administrador.setCodigoSantri("ADM-001");
        administrador.setSenha("hash");
        administrador.setFuncao(Role.ROLE_ADMIN);
        administrador.setAdmin(true);
        administrador.setNome("Administrador Teste");
        administrador.setCPF("10112019501");
        administradorRespository.save(administrador);

        UserDetails resultado = administradorRespository
                .findByCodigoSantriIgnoreCase("adm-001");

        assertThat(resultado).isNotNull();
        assertThat(resultado.getUsername()).isEqualTo("ADM-001");
    }

    @Test
    @Transactional
    @DisplayName("Não deve retornar administrador inexistente")
    void findByCodigoSantriCase2() {
        UserDetails resultado = administradorRespository
                .findByCodigoSantriIgnoreCase("ADM-INEXISTENTE");

        assertThat(resultado).isNull();
    }
}
