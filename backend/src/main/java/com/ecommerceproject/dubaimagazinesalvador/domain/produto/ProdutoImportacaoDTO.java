package com.ecommerceproject.dubaimagazinesalvador.domain.produto;

import java.math.BigDecimal;

public record ProdutoImportacaoDTO(
        String codigoSantri,
        String descricao,
        String ncm,
        String unidade,
        String marca,
        String codigoOriginal,
        BigDecimal quantidade,
        BigDecimal precoVenda,
        BigDecimal precoVendaIva,
        String categoriaCodigo
) {
}
