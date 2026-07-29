package com.ecommerceproject.dubaimagazinesalvador.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.ecommerceproject.dubaimagazinesalvador.domain.categoria.Categoria;
import com.ecommerceproject.dubaimagazinesalvador.repositories.CategoriaRepository;

@ExtendWith(MockitoExtension.class)
class CategoriaControllerTest {

    @Mock
    private CategoriaRepository categoriaRepository;

    private CategoriaController controller;

    @BeforeEach
    void setUp() {
        controller = new CategoriaController(categoriaRepository);
        when(categoriaRepository.findByNivelOrderByNomeAsc(1)).thenReturn(List.of(
                categoria("001", "PAPELARIA"),
                categoria("123", "AJUSTES DE GRUPOS"),
                categoria("999", "IMPLANTACAO")
        ));
    }

    @Test
    void deveOcultarCategoriasInternasParaUsuarioComum() {
        var resposta = controller.getAll(false, 1, null, null);

        assertEquals(List.of("001"), resposta.stream()
                .map(categoria -> categoria.codigo())
                .toList());
    }

    @Test
    void deveExibirCategoriasInternasParaAdministrador() {
        var administrador = new UsernamePasswordAuthenticationToken(
                "admin",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        var resposta = controller.getAll(false, 1, null, administrador);

        assertEquals(List.of("001", "123", "999"), resposta.stream()
                .map(categoria -> categoria.codigo())
                .toList());
    }

    private Categoria categoria(String codigo, String nome) {
        return new Categoria(codigo, nome, 1, nome, null, false);
    }
}
