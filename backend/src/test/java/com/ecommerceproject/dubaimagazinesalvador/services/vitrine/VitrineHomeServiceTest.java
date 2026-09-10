package com.ecommerceproject.dubaimagazinesalvador.services.vitrine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.ecommerceproject.dubaimagazinesalvador.domain.categoria.Categoria;
import com.ecommerceproject.dubaimagazinesalvador.domain.vitrine.VitrineHome;
import com.ecommerceproject.dubaimagazinesalvador.domain.vitrine.VitrineHomeRequestDTO;
import com.ecommerceproject.dubaimagazinesalvador.repositories.CategoriaRepository;
import com.ecommerceproject.dubaimagazinesalvador.repositories.ProdutoRepository;
import com.ecommerceproject.dubaimagazinesalvador.repositories.VitrineHomeRepository;

@ExtendWith(MockitoExtension.class)
class VitrineHomeServiceTest {

    @Test
    void vitrinePublicaNaoRevelaCategoriaInternaMesmoComFlagVisivel() {
        var interna = new Categoria("008.01", "INTERNA", 2, "INTERNA", null, true);
        var ajustes = new Categoria("123.01", "AJUSTES", 2, "AJUSTES", null, true);
        when(vitrineRepository.findByAtivoTrueOrderByOrdemAscIdAsc()).thenReturn(List.of(
                new VitrineHome(interna, "Interno", "Não publicar", 0, true),
                new VitrineHome(ajustes, "Ajustes", "Não publicar", 1, true)
        ));

        assertTrue(service.listarPublicas().isEmpty());
        org.mockito.Mockito.verifyNoInteractions(produtoRepository);
    }

    @Mock
    private VitrineHomeRepository vitrineRepository;

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private ProdutoRepository produtoRepository;

    private VitrineHomeService service;
    private Categoria categoria;

    @BeforeEach
    void setUp() {
        service = new VitrineHomeService(
                vitrineRepository,
                categoriaRepository,
                produtoRepository
        );
        categoria = new Categoria(
                "001",
                "PAPELARIA",
                1,
                "PAPELARIA",
                null,
                false
        );
    }

    @Test
    @DisplayName("Deve criar uma vitrine e amostrar somente produtos públicos da categoria")
    void criarVitrine() {
        VitrineHomeRequestDTO request = new VitrineHomeRequestDTO(
                "001",
                "Volta às aulas",
                "Uma seleção preparada para o novo ano letivo.",
                2,
                true
        );
        when(categoriaRepository.findById("001")).thenReturn(Optional.of(categoria));
        when(vitrineRepository.existsByCategoria_Codigo("001")).thenReturn(false);
        when(vitrineRepository.save(any(VitrineHome.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(produtoRepository.findCatalogoPorCategoria(
                eq("001"),
                isNull(),
                eq(false),
                eq(false),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of()));

        var resposta = service.criar(request);

        assertEquals("PAPELARIA", resposta.categoriaNome());
        assertEquals("Volta às aulas", resposta.titulo());
        assertTrue(resposta.ativo());
        verify(produtoRepository).findCatalogoPorCategoria(
                eq("001"),
                isNull(),
                eq(false),
                eq(false),
                any(Pageable.class)
        );
    }
}
