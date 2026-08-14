package com.ecommerceproject.dubaimagazinesalvador.domain.produto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.ecommerceproject.dubaimagazinesalvador.domain.categoria.Categoria;

class ProdutoTest {

    private final Categoria categoria = new Categoria(
            "001",
            "PAPELARIA",
            1,
            "PAPELARIA",
            null,
            true
    );

    @Test
    void deveCalcularPrecoComIpiSemPersistirValorDuplicado() {
        Produto produto = new Produto(
                produtoImportado("GIZ DE GESSO", "5.49", "13.00"),
                categoria,
                LocalDateTime.now()
        );

        assertEquals(new BigDecimal("6.20"), produto.getPrecoComIpi());
    }

    @Test
    void devePreservarNomePublicoAoAtualizarDadosDoSantri() {
        Produto produto = new Produto(
                produtoImportado("GIZ DE GESSO", "5.49", "13.00"),
                categoria,
                LocalDateTime.now()
        );
        produto.atualizarApresentacao("Giz branco escolar", true, false, null);

        produto.atualizarDadosImportados(
                produtoImportado("GIZ DE GESSO NOVO", "6.49", "13.00"),
                categoria,
                LocalDateTime.now()
        );

        assertEquals("GIZ DE GESSO NOVO", produto.getNome());
        assertEquals("Giz branco escolar", produto.getNomeExibidoSite());
    }

    @Test
    void deveAtualizarImagemPrincipalESecundariaIndependentemente() {
        Produto produto = new Produto(
                produtoImportado("GIZ DE GESSO", "5.49", "13.00"),
                categoria,
                LocalDateTime.now()
        );

        produto.atualizarApresentacao(
                "Giz de gesso",
                true,
                false,
                "/uploads/produtos/principal.webp",
                "/uploads/produtos/hover.webp"
        );

        assertEquals("/uploads/produtos/principal.webp", produto.getImagemUrl());
        assertEquals("/uploads/produtos/hover.webp", produto.getImagemHoverUrl());
    }

    @Test
    void deveRetirarDestaqueAoOcultarProdutoNoSite() {
        Produto produto = new Produto(
                produtoImportado("GIZ DE GESSO", "5.49", "13.00"),
                categoria,
                LocalDateTime.now()
        );
        produto.atualizarApresentacao("Giz de gesso", true, true, null);

        produto.ocultarNoSite();

        assertFalse(produto.isExibirNoSite());
        assertFalse(produto.isDestaqueNaHome());
    }

    @Test
    void deveIdentificarProdutoSemEstoqueComoEsgotado() {
        Produto produto = new Produto(
                new ProdutoRequestDTO(
                        "2672",
                        "GIZ DE GESSO",
                        "GIZ DE GESSO",
                        "96099000",
                        "UN",
                        "DELTA GIZ",
                        null,
                        BigDecimal.ZERO,
                        BigDecimal.TEN,
                        BigDecimal.ZERO,
                        "001",
                        null,
                        true
                ),
                categoria
        );

        assertTrue(produto.isEsgotado());
    }

    private ProdutoImportacaoDTO produtoImportado(
            String nome,
            String preco,
            String percentualIpi
    ) {
        return new ProdutoImportacaoDTO(
                "2672",
                nome,
                "96099000",
                nome,
                "24.316 - DELTA INDUSTRIA E COMERCIO DE GIZ LTDA",
                "DELTA GIZ",
                true,
                "UN",
                "CX",
                LocalDate.of(2019, 8, 1),
                "001000110000001",
                "7897464700019",
                false,
                new BigDecimal("525.000"),
                new BigDecimal(preco),
                new BigDecimal(percentualIpi),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "0 - Nacional",
                null,
                null,
                null,
                null,
                "001"
        );
    }
}
