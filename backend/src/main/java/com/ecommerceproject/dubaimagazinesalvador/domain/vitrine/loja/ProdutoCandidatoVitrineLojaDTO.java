package com.ecommerceproject.dubaimagazinesalvador.domain.vitrine.loja;

import com.ecommerceproject.dubaimagazinesalvador.domain.produto.Produto;

public record ProdutoCandidatoVitrineLojaDTO(
        String codigoSantri,
        String nomeExibidoSite,
        String marca,
        String codigoOriginal,
        String categoriaCodigo,
        String categoriaCaminho,
        boolean exibirNoSite
) {

    public ProdutoCandidatoVitrineLojaDTO(Produto produto) {
        this(
                produto.getCodigoSantri(),
                produto.getNomeExibidoSite(),
                produto.getMarca(),
                produto.getCodigoOriginal(),
                produto.getCategoria().getCodigo(),
                produto.getCategoria().getCaminho(),
                produto.isExibirNoSite()
        );
    }
}

