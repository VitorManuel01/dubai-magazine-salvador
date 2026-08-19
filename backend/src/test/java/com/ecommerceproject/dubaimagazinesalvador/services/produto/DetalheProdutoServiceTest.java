package com.ecommerceproject.dubaimagazinesalvador.services.produto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.ecommerceproject.dubaimagazinesalvador.domain.categoria.Categoria;
import com.ecommerceproject.dubaimagazinesalvador.domain.produto.Produto;
import com.ecommerceproject.dubaimagazinesalvador.domain.produto.ProdutoRequestDTO;
import com.ecommerceproject.dubaimagazinesalvador.repositories.ProdutoRepository;

@ExtendWith(MockitoExtension.class)
class DetalheProdutoServiceTest {

    @Mock
    private ProdutoRepository produtoRepository;

    @Mock
    private ArmazenamentoImagemProdutoService armazenamentoImagem;

    private DetalheProdutoService service;
    private Produto produto;

    @BeforeEach
    void setUp() {
        service = new DetalheProdutoService(produtoRepository, armazenamentoImagem);
        Categoria categoria = new Categoria("001", "ILUMINAÇÃO", 1, "ILUMINAÇÃO", null, true);
        produto = new Produto(
                new ProdutoRequestDTO(
                        "100",
                        "LUSTRE",
                        "Lustre moderno",
                        "9405",
                        "UN",
                        "DUBAI",
                        null,
                        BigDecimal.ONE,
                        new BigDecimal("329.99"),
                        BigDecimal.ZERO,
                        categoria.getCodigo(),
                        null,
                        true
                ),
                categoria
        );
    }

    @Test
    void deveSalvarDescricaoComNegritoETamanhoControlados() {
        when(produtoRepository.findById("100")).thenReturn(Optional.of(produto));
        when(produtoRepository.saveAndFlush(produto)).thenReturn(produto);
        String descricao = "[tamanho=24]**Design moderno**[/tamanho]\n- Bivolt automático";

        service.atualizarDescricao("100", descricao);

        assertEquals(descricao, produto.getDescricaoSite());
    }

    @Test
    void deveRejeitarTamanhoDeFonteForaDaLista() {
        when(produtoRepository.findById("100")).thenReturn(Optional.of(produto));

        ResponseStatusException erro = assertThrows(
                ResponseStatusException.class,
                () -> service.atualizarDescricao(
                        "100",
                        "[tamanho=99]Texto[/tamanho]"
                )
        );

        assertEquals(400, erro.getStatusCode().value());
        verify(produtoRepository, never()).saveAndFlush(produto);
    }

    @Test
    void deveBloquearNonaFotoAntesDeGravarOutroArquivo() {
        produto.substituirImagens(java.util.stream.IntStream.rangeClosed(1, 8)
                .mapToObj(indice -> "/uploads/produtos/" + indice + ".webp")
                .toList());
        when(produtoRepository.findById("100")).thenReturn(Optional.of(produto));
        MockMultipartFile arquivo = new MockMultipartFile(
                "imagem",
                "foto.webp",
                "image/webp",
                new byte[] {1}
        );

        ResponseStatusException erro = assertThrows(
                ResponseStatusException.class,
                () -> service.adicionarImagem("100", arquivo)
        );

        assertEquals(409, erro.getStatusCode().value());
        verify(armazenamentoImagem, never()).salvar(arquivo);
    }
}
