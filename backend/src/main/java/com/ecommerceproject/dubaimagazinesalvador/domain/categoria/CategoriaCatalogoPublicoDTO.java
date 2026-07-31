package com.ecommerceproject.dubaimagazinesalvador.domain.categoria;

public record CategoriaCatalogoPublicoDTO(
        String codigo,
        String nome,
        String caminho
) {

    public CategoriaCatalogoPublicoDTO(Categoria categoria) {
        this(categoria.getCodigo(), categoria.getNome(), categoria.getCaminho());
    }
}
