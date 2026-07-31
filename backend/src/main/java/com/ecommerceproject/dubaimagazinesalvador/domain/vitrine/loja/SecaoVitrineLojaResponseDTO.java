package com.ecommerceproject.dubaimagazinesalvador.domain.vitrine.loja;

public record SecaoVitrineLojaResponseDTO(
        Long id,
        String titulo,
        String conteudo,
        int ordem
) {

    public SecaoVitrineLojaResponseDTO(SecaoVitrineLoja secao) {
        this(secao.getId(), secao.getTitulo(), secao.getConteudo(), secao.getOrdem());
    }
}

