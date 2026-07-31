package com.ecommerceproject.dubaimagazinesalvador.domain.produto;

import java.math.BigDecimal;

public record ProdutoCatalogoPublicoDTO(
        String nomeExibidoSite,
        String marca,
        BigDecimal precoComIpi,
        String categoriaCodigo,
        String categoriaNome,
        String categoriaCaminho,
        String imagemUrl
) {

    public ProdutoCatalogoPublicoDTO(Produto produto) {
        this(
                produto.getNomeExibidoSite(),
                produto.getMarca(),
                produto.getPrecoComIpi(),
                produto.getCategoria().getCodigo(),
                produto.getCategoria().getNome(),
                produto.getCategoria().getCaminho(),
                ImagemProdutoCatalogo.criarUrlPublica(produto.getImagemUrl())
        );
    }
}
