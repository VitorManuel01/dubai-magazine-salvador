package com.ecommerceproject.dubaimagazinesalvador.services.produto;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.ecommerceproject.dubaimagazinesalvador.domain.produto.Produto;
import com.ecommerceproject.dubaimagazinesalvador.domain.vitrine.ControleSelecaoHome;
import com.ecommerceproject.dubaimagazinesalvador.repositories.ControleSelecaoHomeRepository;
import com.ecommerceproject.dubaimagazinesalvador.repositories.ProdutoRepository;

@ExtendWith(MockitoExtension.class)
class ApresentacaoProdutoServiceTest {

    @Mock
    private ProdutoRepository produtoRepository;

    @Mock
    private ControleSelecaoHomeRepository controleRepository;

    @Mock
    private ArmazenamentoImagemProdutoService armazenamentoImagem;

    private ApresentacaoProdutoService service;

    @BeforeEach
    void setUp() {
        service = new ApresentacaoProdutoService(
                produtoRepository,
                controleRepository,
                armazenamentoImagem
        );
    }

    @Test
    void deveRejeitarQuartoProdutoDaSelecaoDaLoja() {
        Produto produto = mock(Produto.class);
        when(controleRepository.bloquearParaAtualizacao())
                .thenReturn(Optional.of(mock(ControleSelecaoHome.class)));
        when(produtoRepository.findById("100")).thenReturn(Optional.of(produto));
        when(produto.isDestaqueNaHome()).thenReturn(false);
        when(produto.isDisponivelUltimaImportacao()).thenReturn(true);
        when(produtoRepository.countByDestaqueNaHomeTrue()).thenReturn(3L);

        assertThatThrownBy(() -> service.atualizar(
                "100", "Produto", true, true, null
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409 CONFLICT");

        verify(produto, never()).atualizarApresentacao(
                "Produto", true, true, null
        );
    }

    @Test
    void devePersistirPrecosJuntoComApresentacaoEPreservarAoDesligar() {
        var produto = new Produto();
        produto.setNome("Produto");
        produto.setNomeExibidoSite("Produto");
        produto.setCategoria(new com.ecommerceproject.dubaimagazinesalvador.domain.categoria.Categoria(
                "001", "Categoria", 1, "Categoria", null, true));
        when(controleRepository.bloquearParaAtualizacao()).thenReturn(Optional.of(mock(ControleSelecaoHome.class)));
        when(produtoRepository.findById("100")).thenReturn(Optional.of(produto));
        when(produtoRepository.saveAndFlush(produto)).thenReturn(produto);
        var resposta = service.atualizar("100", "Produto", true, false, null, null,
                new com.ecommerceproject.dubaimagazinesalvador.domain.produto.PrecosPersonalizadosRequestDTO(
                        true, new java.math.BigDecimal("100.00"), new java.math.BigDecimal("120.00"), 10));
        org.assertj.core.api.Assertions.assertThat(resposta.usarPrecosPersonalizados()).isTrue();
        org.assertj.core.api.Assertions.assertThat(resposta.precoAVista()).isEqualByComparingTo("100.00");
        org.assertj.core.api.Assertions.assertThat(resposta.precoCartaoParc()).isEqualByComparingTo("120.00");
        org.assertj.core.api.Assertions.assertThat(resposta.maxParcelamento()).isEqualTo(10);
        var desligado = service.atualizar("100", "Produto", true, false, null, null,
                new com.ecommerceproject.dubaimagazinesalvador.domain.produto.PrecosPersonalizadosRequestDTO(false, null, null, null));
        org.assertj.core.api.Assertions.assertThat(desligado.usarPrecosPersonalizados()).isFalse();
        org.assertj.core.api.Assertions.assertThat(desligado.precoAVista()).isEqualByComparingTo("100.00");
    }

    @Test
    void deveRejeitarDestaqueDeProdutoOculto() {
        Produto produto = mock(Produto.class);
        when(controleRepository.bloquearParaAtualizacao())
                .thenReturn(Optional.of(mock(ControleSelecaoHome.class)));
        when(produtoRepository.findById("100")).thenReturn(Optional.of(produto));

        assertThatThrownBy(() -> service.atualizar(
                "100", "Produto", false, true, null
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400 BAD_REQUEST");

        verify(produtoRepository, never()).countByDestaqueNaHomeTrue();
    }

    @Test
    void deveRejeitarDestaqueDeProdutoIndisponivelNaUltimaImportacao() {
        Produto produto = mock(Produto.class);
        when(controleRepository.bloquearParaAtualizacao())
                .thenReturn(Optional.of(mock(ControleSelecaoHome.class)));
        when(produtoRepository.findById("100")).thenReturn(Optional.of(produto));
        when(produto.isDisponivelUltimaImportacao()).thenReturn(false);

        assertThatThrownBy(() -> service.atualizar(
                "100", "Produto", true, true, null
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400 BAD_REQUEST");

        verify(produtoRepository, never()).countByDestaqueNaHomeTrue();
    }

    @Test
    void deveFalharFechadoSemRegistroDeControle() {
        when(controleRepository.bloquearParaAtualizacao()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.atualizar(
                "100", "Produto", true, true, null
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("500 INTERNAL_SERVER_ERROR");

        verify(produtoRepository, never()).findById("100");
    }
}
