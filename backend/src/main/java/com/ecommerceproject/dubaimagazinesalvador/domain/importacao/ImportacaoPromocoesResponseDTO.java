package com.ecommerceproject.dubaimagazinesalvador.domain.importacao;

import java.time.LocalDateTime;

public record ImportacaoPromocoesResponseDTO(
        String arquivo,
        int registrosLidos,
        int produtosAtualizados,
        int promocoesAtivas,
        int promocoesDesativadas,
        int codigosNaoEncontrados,
        int linhasIgnoradas,
        LocalDateTime importadoEm,
        long duracaoMs
) {
}
