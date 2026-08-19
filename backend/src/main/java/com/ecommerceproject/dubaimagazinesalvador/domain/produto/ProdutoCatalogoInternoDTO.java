package com.ecommerceproject.dubaimagazinesalvador.domain.produto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ProdutoCatalogoInternoDTO(
        String codigoSantri,
        String idPublico,
        String nomeExibidoSite,
        String marca,
        BigDecimal precoComIpi,
        boolean emPromocao,
        BigDecimal precoPromocao,
        BigDecimal porcDesconto,
        LocalDate dataFinalProm,
        boolean esgotado,
        String categoriaCodigo,
        String categoriaNome,
        String categoriaCaminho,
        String imagemUrl,
        String imagemHoverUrl,
        List<String> imagens
) {
    public ProdutoCatalogoInternoDTO(Produto produto) {
        this(
                produto.getCodigoSantri(),
                produto.getIdPublico(),
                produto.getNomeExibidoSite(),
                produto.getMarca(),
                produto.getPrecoComIpi(),
                produto.isPromocaoVigente(),
                produto.isPromocaoVigente() ? produto.getPrecoPromocao() : null,
                produto.isPromocaoVigente() ? produto.getPorcDesconto() : null,
                produto.isPromocaoVigente() ? produto.getDataFinalProm() : null,
                produto.isEsgotado(),
                produto.getCategoria().getCodigo(),
                produto.getCategoria().getNome(),
                produto.getCategoria().getCaminho(),
                ImagemProdutoCatalogo.criarUrlPublica(produto.getImagemUrl()),
                ImagemProdutoCatalogo.criarUrlPublica(produto.getImagemHoverUrl()),
                produto.getUrlsImagens().stream()
                        .map(ImagemProdutoCatalogo::criarUrlPublica)
                        .toList()
        );
    }
}
