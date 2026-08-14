package com.ecommerceproject.dubaimagazinesalvador.domain.importacao;

import java.time.LocalDateTime;

public record ImportacaoEstoqueResponseDTO(
        String arquivo,
        int registrosLidos,
        int produtosAtualizados,
        int codigosIgnorados,
        LocalDateTime importadoEm,
        long duracaoMilissegundos
) {
}
