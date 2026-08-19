package com.ecommerceproject.dubaimagazinesalvador.domain.vitrine.loja;

import java.math.BigDecimal;

import com.ecommerceproject.dubaimagazinesalvador.domain.produto.Produto;
import com.ecommerceproject.dubaimagazinesalvador.domain.produto.ImagemProdutoCatalogo;

public record ProdutoVitrineLojaProdutoDTO(
        String codigoSantri,
        String nomeExibidoSite,
        String marca,
        String codigoOriginal,
        String unidade,
        BigDecimal quantidade,
        BigDecimal precoVenda,
        String imagemUrl
) {

    public ProdutoVitrineLojaProdutoDTO(Produto produto) {
        this(
                produto.getCodigoSantri(),
                produto.getNomeExibidoSite(),
                produto.getMarca(),
                produto.getCodigoOriginal(),
                produto.getUnidadeVenda(),
                produto.getEstoque(),
                produto.getPrecoVendaEfetivo(),
                ImagemProdutoCatalogo.criarUrlPublica(produto.getImagemUrl())
        );
    }
}
