package com.ecommerceproject.dubaimagazinesalvador.domain.importacao;

import java.time.LocalDateTime;

public record ImportacaoProdutosResponseDTO(
        String arquivo,
        int categoriasLidas,
        int categoriasCriadas,
        int categoriasAtualizadas,
        int produtosLidos,
        int produtosCriados,
        int produtosAtualizados,
        int linhasIgnoradas,
        LocalDateTime importadoEm,
        long duracaoMilissegundos
) {
}
