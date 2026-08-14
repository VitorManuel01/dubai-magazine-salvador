package com.ecommerceproject.dubaimagazinesalvador.domain.importacao;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PromocaoProdutoImportacaoDTO(
        String codigoSantri,
        LocalDate dataInicial,
        LocalDate dataFinal,
        BigDecimal porcMargem,
        BigDecimal porcDesconto,
        BigDecimal precoPromocao,
        boolean especial,
        boolean emPromocao
) {
}
