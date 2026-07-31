package com.ecommerceproject.dubaimagazinesalvador.domain.vitrine;

import java.util.List;

import com.ecommerceproject.dubaimagazinesalvador.domain.produto.ProdutoCatalogoPublicoDTO;

public record VitrineHomeResponseDTO(
        Long id,
        String categoriaCodigo,
        String categoriaNome,
        String categoriaCaminho,
        String titulo,
        String descricao,
        int ordem,
        boolean ativo,
        List<ProdutoCatalogoPublicoDTO> produtos
) {
    public VitrineHomeResponseDTO(
            VitrineHome vitrine,
            List<ProdutoCatalogoPublicoDTO> produtos
    ) {
        this(
                vitrine.getId(),
                vitrine.getCategoria().getCodigo(),
                vitrine.getCategoria().getNome(),
                vitrine.getCategoria().getCaminho(),
                vitrine.getTitulo(),
                vitrine.getDescricao(),
                vitrine.getOrdem(),
                vitrine.isAtivo(),
                produtos
        );
    }
}
