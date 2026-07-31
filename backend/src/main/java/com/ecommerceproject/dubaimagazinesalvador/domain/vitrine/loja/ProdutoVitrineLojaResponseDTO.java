package com.ecommerceproject.dubaimagazinesalvador.domain.vitrine.loja;

import java.util.List;

public record ProdutoVitrineLojaResponseDTO(
        Long id,
        ProdutoVitrineLojaProdutoDTO produto,
        String rotuloOpcao,
        int ordem,
        List<String> imagens,
        List<SecaoVitrineLojaResponseDTO> secoes
) {

    public ProdutoVitrineLojaResponseDTO(ProdutoVitrineLoja opcao) {
        this(
                opcao.getId(),
                new ProdutoVitrineLojaProdutoDTO(opcao.getProduto()),
                opcao.getRotuloOpcao(),
                opcao.getOrdem(),
                List.copyOf(opcao.getImagens()),
                opcao.getSecoes().stream()
                        .map(SecaoVitrineLojaResponseDTO::new)
                        .toList()
        );
    }
}

