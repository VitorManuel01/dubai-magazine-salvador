package com.ecommerceproject.dubaimagazinesalvador.domain.vitrine.loja;

import java.time.LocalDateTime;
import java.util.List;

public record VitrineLojaResponseDTO(
        Long id,
        boolean ativo,
        List<ProdutoVitrineLojaResponseDTO> opcoes,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm
) {

    public VitrineLojaResponseDTO(VitrineLoja vitrine) {
        this(
                vitrine.getId(),
                vitrine.isAtivo(),
                vitrine.getOpcoes().stream()
                        .map(ProdutoVitrineLojaResponseDTO::new)
                        .toList(),
                vitrine.getCriadoEm(),
                vitrine.getAtualizadoEm()
        );
    }
}

