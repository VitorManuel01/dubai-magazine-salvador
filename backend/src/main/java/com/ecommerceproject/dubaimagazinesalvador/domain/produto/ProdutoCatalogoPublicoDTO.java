package com.ecommerceproject.dubaimagazinesalvador.domain.produto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ProdutoCatalogoPublicoDTO(
        String idPublico,
        String nomeExibidoSite,
        String marca,
        BigDecimal precoComIpi,
        boolean usarPrecosPersonalizados,
        BigDecimal precoAVista,
        BigDecimal precoCartaoParc,
        Integer maxParcelamento,
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

    public ProdutoCatalogoPublicoDTO(Produto produto) {
        this(
                produto.getIdPublico(),
                produto.getNomeExibidoSite(),
                produto.getMarca(),
                produto.getPrecoComIpi(),
                produto.isUsarPrecosPersonalizados(),
                produto.isUsarPrecosPersonalizados() ? produto.getPrecoAVista() : null,
                produto.isUsarPrecosPersonalizados() ? produto.getPrecoCartaoParc() : null,
                produto.isUsarPrecosPersonalizados() ? produto.getMaxParcelamento() : null,
                produto.isPromocaoExibidaSite(),
                produto.isPromocaoExibidaSite() ? produto.getPrecoPromocao() : null,
                produto.isPromocaoExibidaSite() ? produto.getPorcDesconto() : null,
                produto.isPromocaoExibidaSite() ? produto.getDataFinalProm() : null,
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
