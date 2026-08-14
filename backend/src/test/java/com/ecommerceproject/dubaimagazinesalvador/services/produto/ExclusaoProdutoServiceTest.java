package com.ecommerceproject.dubaimagazinesalvador.services.produto;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.ecommerceproject.dubaimagazinesalvador.domain.produto.Produto;
import com.ecommerceproject.dubaimagazinesalvador.repositories.ProdutoRepository;

@ExtendWith(MockitoExtension.class)
class ExclusaoProdutoServiceTest {

    @Mock
    private ProdutoRepository produtoRepository;

    @Mock
    private ArmazenamentoImagemProdutoService armazenamentoImagem;

    @Test
    void deveExcluirProdutoELimparImagensGerenciadas() {
        Produto produto = mock(Produto.class);
        when(produto.getImagemUrl()).thenReturn("/uploads/produtos/principal.webp");
        when(produto.getImagemHoverUrl()).thenReturn("/uploads/produtos/hover.webp");
        when(produtoRepository.findById("100")).thenReturn(Optional.of(produto));

        new ExclusaoProdutoService(produtoRepository, armazenamentoImagem).excluir("100");

        verify(produtoRepository).delete(produto);
        verify(produtoRepository).flush();
        verify(armazenamentoImagem).removerSeGerenciada("/uploads/produtos/principal.webp");
        verify(armazenamentoImagem).removerSeGerenciada("/uploads/produtos/hover.webp");
    }

    @Test
    void deveResponderNaoEncontradoParaCodigoInexistente() {
        when(produtoRepository.findById("999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new ExclusaoProdutoService(
                produtoRepository,
                armazenamentoImagem
        ).excluir("999"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Produto não encontrado");
    }

    @Test
    void deveExcluirTodosOsProdutosSelecionadosAposValidarTodosOsCodigos() {
        Produto primeiro = mock(Produto.class);
        Produto segundo = mock(Produto.class);
        when(primeiro.getImagemUrl()).thenReturn("/uploads/produtos/100.webp");
        when(segundo.getImagemHoverUrl()).thenReturn("/uploads/produtos/200-hover.webp");
        when(produtoRepository.findAllById(any())).thenReturn(List.of(primeiro, segundo));

        int excluidos = new ExclusaoProdutoService(
                produtoRepository,
                armazenamentoImagem
        ).excluirTodos(List.of("100", "200"));

        org.junit.jupiter.api.Assertions.assertEquals(2, excluidos);
        verify(produtoRepository).deleteAll(List.of(primeiro, segundo));
        verify(produtoRepository).flush();
        verify(armazenamentoImagem).removerSeGerenciada("/uploads/produtos/100.webp");
        verify(armazenamentoImagem).removerSeGerenciada("/uploads/produtos/200-hover.webp");
    }

    @Test
    void naoDeveExcluirParcialmenteQuandoUmCodigoSelecionadoNaoExiste() {
        Produto produto = mock(Produto.class);
        when(produto.getCodigoSantri()).thenReturn("100");
        when(produtoRepository.findAllById(any())).thenReturn(List.of(produto));

        assertThatThrownBy(() -> new ExclusaoProdutoService(
                produtoRepository,
                armazenamentoImagem
        ).excluirTodos(List.of("100", "999")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Produto não encontrado: 999");

        verify(produtoRepository, never()).deleteAll(any(Iterable.class));
    }
}
