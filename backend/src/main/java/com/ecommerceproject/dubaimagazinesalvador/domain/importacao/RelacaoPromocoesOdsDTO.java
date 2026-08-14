package com.ecommerceproject.dubaimagazinesalvador.domain.importacao;

import java.util.List;

public record RelacaoPromocoesOdsDTO(
        List<PromocaoProdutoImportacaoDTO> promocoes,
        int linhasIgnoradas
) {
}
