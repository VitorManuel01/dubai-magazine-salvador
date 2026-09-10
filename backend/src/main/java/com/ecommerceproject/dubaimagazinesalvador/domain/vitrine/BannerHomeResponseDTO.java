package com.ecommerceproject.dubaimagazinesalvador.domain.vitrine;

import com.ecommerceproject.dubaimagazinesalvador.domain.produto.ImagemProdutoCatalogo;

public record BannerHomeResponseDTO(
        int posicao,
        String imagemUrl
) {
    public BannerHomeResponseDTO(BannerHome banner) {
        this(banner.getPosicao(), ImagemProdutoCatalogo.criarUrlPublica(banner.getImagemUrl()));
    }
}
