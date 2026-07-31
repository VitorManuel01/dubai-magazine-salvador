package com.ecommerceproject.dubaimagazinesalvador.domain.produto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Dados de um produto vindos da relação analítica do Santri.
 *
 * Os campos de apresentação do site não fazem parte deste DTO para que uma
 * importação nunca sobrescreva nome público, imagem, visibilidade ou destaque.
 */
public record ProdutoImportacaoDTO(
        String codigoSantri,
        String nome,
        String ncm,
        String nomeCompra,
        String fabricante,
        String marca,
        boolean ativoSantri,
        String unidadeVenda,
        String unidadeCompra,
        LocalDate dataCadastro,
        String codigoOriginal,
        String codigoBarras,
        boolean bloqueadoParaCompras,
        BigDecimal estoque,
        BigDecimal precoSemIpi,
        BigDecimal percentualIpiEntrada,
        BigDecimal pesoUnidade,
        BigDecimal alturaUnidade,
        BigDecimal larguraUnidade,
        BigDecimal comprimentoUnidade,
        BigDecimal volumeUnidadeM3,
        BigDecimal volumeLitros,
        BigDecimal pesoCaixa,
        BigDecimal alturaCaixa,
        BigDecimal larguraCaixa,
        BigDecimal comprimentoCaixa,
        String origem,
        Boolean industrializado,
        Boolean insumo,
        BigDecimal percentualMaximoAproveitamentoIpi,
        String numeroFci,
        String categoriaCodigo
) {
}
