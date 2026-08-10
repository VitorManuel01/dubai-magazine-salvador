package com.ecommerceproject.dubaimagazinesalvador.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import com.ecommerceproject.dubaimagazinesalvador.services.produto.ArmazenamentoImagemProdutoService;

class ImagemVitrineLojaControllerTest {

    @Test
    void deveRetornarUrlPublicaAposArmazenarImagem() {
        ArmazenamentoImagemProdutoService armazenamento = mock(
                ArmazenamentoImagemProdutoService.class
        );
        ImagemVitrineLojaController controller = new ImagemVitrineLojaController(
                armazenamento
        );
        MockMultipartFile imagem = new MockMultipartFile(
                "imagem",
                "produto.webp",
                "image/webp",
                "imagem".getBytes(StandardCharsets.UTF_8)
        );
        String identificador = "123e4567-e89b-12d3-a456-426614174000.webp";
        when(armazenamento.salvar(imagem))
                .thenReturn("/uploads/produtos/" + identificador);

        var resposta = controller.enviar(imagem);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resposta.getBody()).isNotNull();
        assertThat(resposta.getBody().url())
                .isEqualTo("/catalogo/imagens/" + identificador);
    }
}
