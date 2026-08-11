package com.ecommerceproject.dubaimagazinesalvador.domain.vitrine;

public record BannerHomeResponseDTO(
        int posicao,
        String imagemUrl
) {
    public BannerHomeResponseDTO(BannerHome banner) {
        this(banner.getPosicao(), banner.getImagemUrl());
    }
}
