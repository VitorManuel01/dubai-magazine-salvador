package com.ecommerceproject.dubaimagazinesalvador.domain.importacao;

import java.math.BigDecimal;

public record EstoqueProdutoImportacaoDTO(
        String codigoSantri,
        BigDecimal quantidade
) {
}
