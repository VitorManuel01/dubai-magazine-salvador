package com.ecommerceproject.dubaimagazinesalvador.controller;

import java.time.Duration;

import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerceproject.dubaimagazinesalvador.services.produto.ArmazenamentoImagemProdutoService;

@RestController
public class ImagemCatalogoController {

    private final ArmazenamentoImagemProdutoService armazenamentoImagem;

    public ImagemCatalogoController(ArmazenamentoImagemProdutoService armazenamentoImagem) {
        this.armazenamentoImagem = armazenamentoImagem;
    }

    @GetMapping("/catalogo/imagens/{nomeArquivo}")
    public ResponseEntity<Resource> carregar(@PathVariable String nomeArquivo) {
        Resource imagem = armazenamentoImagem.carregarParaCatalogo(nomeArquivo);
        return ResponseEntity.ok()
                .contentType(tipoMidia(nomeArquivo))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                .body(imagem);
    }

    private MediaType tipoMidia(String nomeArquivo) {
        String nome = nomeArquivo.toLowerCase();
        if (nome.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (nome.endsWith(".webp")) {
            return MediaType.parseMediaType("image/webp");
        }
        return MediaType.IMAGE_JPEG;
    }
}
