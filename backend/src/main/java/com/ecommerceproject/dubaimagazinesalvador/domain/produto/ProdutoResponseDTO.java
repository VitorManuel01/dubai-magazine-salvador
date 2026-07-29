package com.ecommerceproject.dubaimagazinesalvador.domain.produto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProdutoResponseDTO(
        String codigoSantri,
        String descricao,
        String nomeExibidoSite,
        String ncm,
        String unidade,
        String marca,
        String codigoOriginal,
        BigDecimal quantidade,
        BigDecimal precoVenda,
        BigDecimal precoVendaIva,
        String categoriaCodigo,
        String categoriaNome,
        String categoriaCaminho,
        String imagemUrl,
        boolean exibirNoSite,
        boolean destaqueNaHome,
        LocalDateTime ultimaImportacaoEm
) {

    public ProdutoResponseDTO(Produto produto) {
        this(
                produto.getCodigoSantri(),
                produto.getDescricao(),
                produto.getNomeExibidoSite(),
                produto.getNcm(),
                produto.getUnidade(),
                produto.getMarca(),
                produto.getCodigoOriginal(),
                produto.getQuantidade(),
                produto.getPrecoVenda(),
                produto.getPrecoVendaIva(),
                produto.getCategoria().getCodigo(),
                produto.getCategoria().getNome(),
                produto.getCategoria().getCaminho(),
                produto.getImagemUrl(),
                produto.isExibirNoSite(),
                produto.isDestaqueNaHome(),
                produto.getUltimaImportacaoEm()
        );
    }
}
