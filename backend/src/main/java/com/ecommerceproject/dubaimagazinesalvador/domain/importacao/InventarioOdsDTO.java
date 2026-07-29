package com.ecommerceproject.dubaimagazinesalvador.domain.importacao;

import java.util.List;

import com.ecommerceproject.dubaimagazinesalvador.domain.produto.ProdutoImportacaoDTO;

public record InventarioOdsDTO(
        List<CategoriaImportacaoDTO> categorias,
        List<ProdutoImportacaoDTO> produtos,
        int linhasIgnoradas
) {
}
