package com.ecommerceproject.dubaimagazinesalvador.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ecommerceproject.dubaimagazinesalvador.domain.produto.ImagemProdutoCatalogo;
import com.ecommerceproject.dubaimagazinesalvador.domain.vitrine.loja.ImagemVitrineLojaUploadDTO;
import com.ecommerceproject.dubaimagazinesalvador.services.produto.ArmazenamentoImagemProdutoService;

@RestController
public class ImagemVitrineLojaController {

    private final ArmazenamentoImagemProdutoService armazenamentoImagem;

    public ImagemVitrineLojaController(
            ArmazenamentoImagemProdutoService armazenamentoImagem
    ) {
        this.armazenamentoImagem = armazenamentoImagem;
    }

    @PostMapping(
            value = "/admin/vitrine-loja/imagens",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ImagemVitrineLojaUploadDTO> enviar(
            @RequestParam("imagem") MultipartFile imagem
    ) {
        String caminhoArmazenado = armazenamentoImagem.salvar(imagem);
        String urlPublica = ImagemProdutoCatalogo.criarUrlPublica(caminhoArmazenado);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ImagemVitrineLojaUploadDTO(urlPublica));
    }
}
