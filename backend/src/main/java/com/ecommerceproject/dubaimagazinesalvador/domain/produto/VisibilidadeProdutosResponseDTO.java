package com.ecommerceproject.dubaimagazinesalvador.domain.produto;

public record VisibilidadeProdutosResponseDTO(
        int produtosSelecionados,
        int produtosAlterados,
        boolean exibirNoSite
) {
}
