package com.ecommerceproject.dubaimagazinesalvador.domain.produto;

public record ProdutoImagemDTO(Long id, String url, int ordem) {

    public ProdutoImagemDTO(ProdutoImagem imagem) {
        this(
                imagem.getId(),
                ImagemProdutoCatalogo.criarUrlPublica(imagem.getUrl()),
                imagem.getOrdem()
        );
    }
}
