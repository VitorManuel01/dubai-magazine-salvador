package com.ecommerceproject.dubaimagazinesalvador.domain.vitrine.loja;

import java.util.List;

public record VitrineLojaRequestDTO(
        Boolean ativo,
        List<ProdutoVitrineLojaRequestDTO> opcoes
) {
}

