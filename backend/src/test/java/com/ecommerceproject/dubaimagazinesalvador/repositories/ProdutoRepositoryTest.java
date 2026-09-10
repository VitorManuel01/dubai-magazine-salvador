package com.ecommerceproject.dubaimagazinesalvador.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.ecommerceproject.dubaimagazinesalvador.domain.categoria.Categoria;
import com.ecommerceproject.dubaimagazinesalvador.domain.produto.Produto;
import com.ecommerceproject.dubaimagazinesalvador.domain.produto.ProdutoRequestDTO;
import com.ecommerceproject.dubaimagazinesalvador.infra.security.SecurityFilter;
import com.ecommerceproject.dubaimagazinesalvador.infra.security.TokenService;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProdutoRepositoryTest {

    private static final String CATEGORIA_CODIGO = "TEST-PRECO-PAGINACAO";

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private SecurityFilter securityFilter;

    @Test
    @DisplayName("Deve filtrar por preço antes de montar as páginas")
    void findCatalogoPorFaixaDePreco() {
        Categoria categoria = categoriaRepository.save(new Categoria(
                CATEGORIA_CODIGO,
                "Teste de preço",
                1,
                "Teste de preço",
                null,
                true
        ));

        List<Produto> produtos = new ArrayList<>();
        for (int indice = 1; indice <= 30; indice++) {
            produtos.add(criarProduto(
                    "TEST-BARATO-" + indice,
                    "Produto barato " + indice,
                    new BigDecimal("1.99"),
                    categoria
            ));
        }
        for (int indice = 1; indice <= 5; indice++) {
            produtos.add(criarProduto(
                    "TEST-CARO-" + indice,
                    "Produto caro " + indice,
                    new BigDecimal("10.00"),
                    categoria
            ));
        }
        produtoRepository.saveAllAndFlush(produtos);

        Page<Produto> primeiraPagina = produtoRepository.findCatalogoPorCategoria(
                CATEGORIA_CODIGO,
                null,
                false,
                false,
                null,
                new BigDecimal("2.00"),
                PageRequest.of(0, 24)
        );
        Page<Produto> segundaPagina = produtoRepository.findCatalogoPorCategoria(
                CATEGORIA_CODIGO,
                null,
                false,
                false,
                null,
                new BigDecimal("2.00"),
                PageRequest.of(1, 24)
        );

        assertThat(primeiraPagina.getTotalElements()).isEqualTo(30);
        assertThat(primeiraPagina.getTotalPages()).isEqualTo(2);
        assertThat(primeiraPagina.getContent()).hasSize(24);
        assertThat(segundaPagina.getContent()).hasSize(6);
        assertThat(primeiraPagina.getContent())
                .allMatch(produto -> produto.getPrecoComIpi().compareTo(new BigDecimal("2.00")) <= 0);
    }

    @Test
    void filtroDePrecoConsideraValorPersonalizadoEVoltaAoSantriQuandoDesligado() {
        var categoria = categoriaRepository.save(new Categoria(CATEGORIA_CODIGO, "Teste", 1, "Teste", null, true));
        var manual = criarProduto("TEST-MANUAL", "Manual", new BigDecimal("100.00"), categoria);
        new com.ecommerceproject.dubaimagazinesalvador.domain.produto.PrecosPersonalizadosRequestDTO(
                true, new BigDecimal("1.50"), new BigDecimal("2.00"), 2).aplicar(manual);
        produtoRepository.saveAndFlush(manual);
        var pagina = produtoRepository.findCatalogoPorCategoria(CATEGORIA_CODIGO, null, false, false,
                BigDecimal.ONE, new BigDecimal("2.00"), PageRequest.of(0, 24));
        assertThat(pagina.getTotalElements()).isEqualTo(1);
        assertThat(pagina.getContent()).extracting(Produto::getCodigoSantri).containsExactly("TEST-MANUAL");
        manual.setUsarPrecosPersonalizados(false);
        produtoRepository.saveAndFlush(manual);
        assertThat(produtoRepository.findCatalogoPorCategoria(CATEGORIA_CODIGO, null, false, false,
                BigDecimal.ONE, new BigDecimal("2.00"), PageRequest.of(0, 24)).getTotalElements()).isZero();
    }

    private Produto criarProduto(
            String codigo,
            String nome,
            BigDecimal preco,
            Categoria categoria
    ) {
        return new Produto(new ProdutoRequestDTO(
                codigo,
                nome,
                nome,
                null,
                "UN",
                "MARCA TESTE",
                null,
                BigDecimal.ONE,
                preco,
                BigDecimal.ZERO,
                categoria.getCodigo(),
                null,
                true
        ), categoria);
    }
}
