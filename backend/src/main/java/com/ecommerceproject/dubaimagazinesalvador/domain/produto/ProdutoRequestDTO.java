package com.ecommerceproject.dubaimagazinesalvador.domain.produto;

import java.math.BigDecimal;

public record ProdutoRequestDTO(String nome, BigDecimal preco, int qtdEstoque, String categoria, String imagemUrl) {

}
