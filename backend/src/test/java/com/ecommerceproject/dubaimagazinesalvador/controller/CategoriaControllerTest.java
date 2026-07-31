package com.ecommerceproject.dubaimagazinesalvador.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ecommerceproject.dubaimagazinesalvador.domain.categoria.Categoria;
import com.ecommerceproject.dubaimagazinesalvador.repositories.CategoriaRepository;

@ExtendWith(MockitoExtension.class)
class CategoriaControllerTest {

    @Mock
    private CategoriaRepository categoriaRepository;

    private CategoriaController controller;
    private List<Categoria> categorias;

    @BeforeEach
    void setUp() {
        controller = new CategoriaController(categoriaRepository);
        categorias = List.of(
                categoria("001", "PAPELARIA"),
                categoria("123", "AJUSTES DE GRUPOS"),
                categoria("999", "IMPLANTACAO")
        );
    }

    @Test
    void deveOcultarCategoriasInternasParaUsuarioComum() {
        when(categoriaRepository.findByNivelAndExibirNoSiteTrueOrderByNomeAsc(1))
                .thenReturn(categorias);
        var resposta = controller.getAll(1, null);

        assertEquals(List.of("001"), resposta.stream()
                .map(categoria -> categoria.codigo())
                .toList());
    }

    @Test
    void deveExibirCategoriasInternasParaAdministrador() {
        when(categoriaRepository.findByNivelOrderByNomeAsc(1)).thenReturn(categorias);
        var resposta = controller.getAllAdministracao(false, 1, null);

        assertEquals(List.of("001", "123", "999"), resposta.stream()
                .map(categoria -> categoria.codigo())
                .toList());
    }

    private Categoria categoria(String codigo, String nome) {
        return new Categoria(codigo, nome, 1, nome, null, false);
    }
}
