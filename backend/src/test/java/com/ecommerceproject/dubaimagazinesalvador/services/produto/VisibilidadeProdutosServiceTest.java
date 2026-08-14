package com.ecommerceproject.dubaimagazinesalvador.services.produto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.ecommerceproject.dubaimagazinesalvador.domain.produto.Produto;
import com.ecommerceproject.dubaimagazinesalvador.domain.produto.VisibilidadeProdutosRequestDTO;
import com.ecommerceproject.dubaimagazinesalvador.repositories.ProdutoRepository;

@ExtendWith(MockitoExtension.class)
class VisibilidadeProdutosServiceTest {

    @Mock
    private ProdutoRepository produtoRepository;

    @Test
    void deveExibirTodosOsProdutosSelecionadosDisponiveis() {
        Produto oculto = produto("100", true, false);
        Produto visivel = produto("200", true, true);
        when(produtoRepository.findAllById(any()))
                .thenReturn(List.of(oculto, visivel));

        var resposta = new VisibilidadeProdutosService(produtoRepository)
                .alterarVisibilidade(new VisibilidadeProdutosRequestDTO(
                        List.of("100", "200"),
                        true
                ));

        assertThat(resposta.produtosSelecionados()).isEqualTo(2);
        assertThat(resposta.produtosAlterados()).isEqualTo(1);
        assertThat(resposta.exibirNoSite()).isTrue();
        verify(oculto).tornarVisivelNoSite();
        verify(visivel, never()).tornarVisivelNoSite();
    }

    @Test
    void deveRejeitarProdutoIndisponivelSemAlterarOsDemais() {
        Produto disponivel = produto("100", true, false);
        Produto indisponivel = produto("200", false, false);
        when(produtoRepository.findAllById(any()))
                .thenReturn(List.of(disponivel, indisponivel));

        assertThatThrownBy(() -> new VisibilidadeProdutosService(produtoRepository)
                .alterarVisibilidade(new VisibilidadeProdutosRequestDTO(
                        List.of("100", "200"),
                        true
                )))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("indisponíveis");

        verify(disponivel, never()).tornarVisivelNoSite();
        verify(indisponivel, never()).tornarVisivelNoSite();
    }

    @Test
    void deveOcultarSelecionadosMesmoQuandoIndisponiveis() {
        Produto visivel = produto("100", true, true);
        Produto indisponivelVisivel = produto("200", false, true);
        Produto oculto = produto("300", true, false);
        when(produtoRepository.findAllById(any()))
                .thenReturn(List.of(visivel, indisponivelVisivel, oculto));

        var resposta = new VisibilidadeProdutosService(produtoRepository)
                .alterarVisibilidade(new VisibilidadeProdutosRequestDTO(
                        List.of("100", "200", "300"),
                        false
                ));

        assertThat(resposta.produtosSelecionados()).isEqualTo(3);
        assertThat(resposta.produtosAlterados()).isEqualTo(2);
        assertThat(resposta.exibirNoSite()).isFalse();
        verify(visivel).ocultarNoSite();
        verify(indisponivelVisivel).ocultarNoSite();
        verify(oculto, never()).ocultarNoSite();
    }

    private Produto produto(String codigo, boolean disponivel, boolean visivel) {
        Produto produto = mock(Produto.class);
        lenient().when(produto.getCodigoSantri()).thenReturn(codigo);
        lenient().when(produto.isDisponivelUltimaImportacao()).thenReturn(disponivel);
        lenient().when(produto.isExibirNoSite()).thenReturn(visivel);
        return produto;
    }
}
