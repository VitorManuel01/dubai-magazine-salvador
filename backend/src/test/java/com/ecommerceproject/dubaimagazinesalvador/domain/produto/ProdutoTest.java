package com.ecommerceproject.dubaimagazinesalvador.domain.produto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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
    void precosManuaisPrevalecemSemAlterarSantriESobrevivemAImportacao() {
        Produto produto = new Produto(produtoImportado("GIZ", "10.00", "10"), categoria, LocalDateTime.now());
        produto.setEmPromocao(true);
        produto.setPrecoPromocao(new BigDecimal("9.00"));
        new PrecosPersonalizadosRequestDTO(true, new BigDecimal("7.50"), new BigDecimal("9.00"), 3).aplicar(produto);
        assertEquals(new BigDecimal("7.50"), produto.getPrecoVendaEfetivo());
        assertEquals(new BigDecimal("11.00"), produto.getPrecoComIpi());
        assertFalse(new ProdutoCatalogoPublicoDTO(produto).emPromocao());
        assertEquals(new BigDecimal("7.50"), new ProdutoDetalhePublicoDTO(produto).precoAVista());
        assertEquals(3, new ProdutoCatalogoInternoDTO(produto).maxParcelamento());
        assertEquals(new BigDecimal("7.50"), new com.ecommerceproject.dubaimagazinesalvador.domain.vitrine.loja.ProdutoVitrineLojaProdutoDTO(produto).precoVenda());

        produto.atualizarDadosImportados(produtoImportado("GIZ NOVO", "20.00", "10"), categoria, LocalDateTime.now());
        assertEquals(new BigDecimal("22.00"), produto.getPrecoComIpi());
        assertEquals(new BigDecimal("7.50"), produto.getPrecoVendaEfetivo());
        assertEquals(new BigDecimal("9.00"), produto.getPrecoCartaoParc());
        assertTrue(produto.isUsarPrecosPersonalizados());

        new PrecosPersonalizadosRequestDTO(false, null, null, null).aplicar(produto);
        assertEquals(new BigDecimal("9.00"), produto.getPrecoVendaEfetivo());
        assertTrue(new ProdutoCatalogoPublicoDTO(produto).emPromocao());
        assertEquals(null, new ProdutoCatalogoPublicoDTO(produto).precoAVista());
        assertEquals(new BigDecimal("7.50"), new ProdutoResponseDTO(produto).precoAVista());
        produto.setEmPromocao(false);
        assertEquals(new BigDecimal("22.00"), produto.getPrecoVendaEfetivo());
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
                "/uploads/produtos/11111111-1111-1111-1111-111111111111.webp",
                "/uploads/produtos/22222222-2222-2222-2222-222222222222.webp"
        );

        assertEquals("/uploads/produtos/11111111-1111-1111-1111-111111111111.webp", produto.getImagemUrl());
        assertEquals("/uploads/produtos/22222222-2222-2222-2222-222222222222.webp", produto.getImagemHoverUrl());
    }

    @Test
    void deveUsarAsDuasPrimeirasFotosDaGaleriaNoCardELimitarEmOito() {
        Produto produto = new Produto(
                produtoImportado("GIZ DE GESSO", "5.49", "13.00"),
                categoria,
                LocalDateTime.now()
        );
        List<String> imagens = java.util.stream.IntStream.rangeClosed(1, 8)
                .mapToObj(indice -> String.format(
                        "/uploads/produtos/00000000-0000-0000-0000-%012d.webp",
                        indice
                ))
                .toList();

        produto.substituirImagens(imagens);

        assertEquals(imagens, produto.getUrlsImagens());
        assertEquals(imagens.getFirst(), produto.getImagemUrl());
        assertEquals(imagens.get(1), produto.getImagemHoverUrl());
        assertThrows(
                IllegalStateException.class,
                () -> produto.adicionarImagem(
                        "/uploads/produtos/00000000-0000-0000-0000-000000000009.webp"
                )
        );
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
