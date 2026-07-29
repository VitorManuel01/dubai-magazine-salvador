package com.ecommerceproject.dubaimagazinesalvador.domain.importacao;

public record CategoriaImportacaoDTO(
        String codigo,
        String nome,
        int nivel,
        String caminho,
        String categoriaPaiCodigo
) {
}
