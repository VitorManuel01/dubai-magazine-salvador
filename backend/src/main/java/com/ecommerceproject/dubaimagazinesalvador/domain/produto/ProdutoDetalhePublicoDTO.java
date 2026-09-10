package com.ecommerceproject.dubaimagazinesalvador.domain.produto;

import java.math.BigDecimal;
import java.util.List;

public record ProdutoDetalhePublicoDTO(
        String idPublico,
        String nomeExibidoSite,
        BigDecimal precoComIpi,
        boolean usarPrecosPersonalizados,
        BigDecimal precoAVista,
        BigDecimal precoCartaoParc,
        Integer maxParcelamento,
        boolean emPromocao,
        BigDecimal precoPromocao,
        BigDecimal porcDesconto,
        boolean esgotado,
        String categoriaNome,
        String categoriaCaminho,
        List<String> imagens,
        String descricao
) {

    public ProdutoDetalhePublicoDTO(Produto produto) {
        this(
                produto.getIdPublico(),
                produto.getNomeExibidoSite(),
                produto.getPrecoComIpi(),
                produto.isUsarPrecosPersonalizados(),
                produto.isUsarPrecosPersonalizados() ? produto.getPrecoAVista() : null,
                produto.isUsarPrecosPersonalizados() ? produto.getPrecoCartaoParc() : null,
                produto.isUsarPrecosPersonalizados() ? produto.getMaxParcelamento() : null,
                produto.isPromocaoExibidaSite(),
                produto.isPromocaoExibidaSite() ? produto.getPrecoPromocao() : null,
                produto.isPromocaoExibidaSite() ? produto.getPorcDesconto() : null,
                produto.isEsgotado(),
                produto.getCategoria().getNome(),
                produto.getCategoria().getCaminho(),
                produto.getUrlsImagens().stream()
                        .map(ImagemProdutoCatalogo::criarUrlPublica)
                        .toList(),
                produto.getDescricaoSite()
        );
    }
}
