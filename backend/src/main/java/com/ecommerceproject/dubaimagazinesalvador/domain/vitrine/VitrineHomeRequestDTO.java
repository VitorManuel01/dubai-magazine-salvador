package com.ecommerceproject.dubaimagazinesalvador.domain.vitrine;

public record VitrineHomeRequestDTO(
        String categoriaCodigo,
        String titulo,
        String descricao,
        Integer ordem,
        boolean ativo
) {
}
