package com.ecommerceproject.dubaimagazinesalvador.domain.vitrine.loja;

import java.util.List;

public record ProdutoVitrineLojaRequestDTO(
        String produtoCodigoSantri,
        String rotuloOpcao,
        Integer ordem,
        List<String> imagens,
        List<SecaoVitrineLojaRequestDTO> secoes
) {
}

