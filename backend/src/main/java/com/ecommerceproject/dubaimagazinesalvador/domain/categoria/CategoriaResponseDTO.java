package com.ecommerceproject.dubaimagazinesalvador.domain.categoria;

public record CategoriaResponseDTO(
        String codigo,
        String nome,
        int nivel,
        String caminho,
        String categoriaPaiCodigo,
        boolean exibirNoSite
) {
    public CategoriaResponseDTO(Categoria categoria) {
        this(
                categoria.getCodigo(),
                categoria.getNome(),
                categoria.getNivel(),
                categoria.getCaminho(),
                categoria.getCategoriaPai() == null ? null : categoria.getCategoriaPai().getCodigo(),
                categoria.isExibirNoSite()
        );
    }
}
