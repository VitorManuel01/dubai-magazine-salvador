package com.ecommerceproject.dubaimagazinesalvador.domain.vitrine;

public record DepoimentoHomeResponseDTO(
        int posicao,
        String nome,
        String texto
) {
    public DepoimentoHomeResponseDTO(DepoimentoHome depoimento) {
        this(depoimento.getPosicao(), depoimento.getNome(), depoimento.getTexto());
    }
}
