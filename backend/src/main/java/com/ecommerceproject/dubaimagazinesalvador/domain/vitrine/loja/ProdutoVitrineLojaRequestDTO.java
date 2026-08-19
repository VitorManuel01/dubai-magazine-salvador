package com.ecommerceproject.dubaimagazinesalvador.domain.vitrine.loja;

import java.util.List;

public record ProdutoVitrineLojaRequestDTO(
        String produtoCodigoSantri,
        String rotuloOpcao,
        Integer ordem,
        List<String> imagens,
        Boolean atualizarImagens,
        List<String> imagensOriginais,
        List<SecaoVitrineLojaRequestDTO> secoes
) {
    public ProdutoVitrineLojaRequestDTO(
            String produtoCodigoSantri,
            String rotuloOpcao,
            Integer ordem,
            List<String> imagens,
            List<SecaoVitrineLojaRequestDTO> secoes
    ) {
        this(produtoCodigoSantri, rotuloOpcao, ordem, imagens, true, null, secoes);
    }
}
