package com.ecommerceproject.dubaimagazinesalvador.domain.produto;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "imagens_produto")
@Getter
@NoArgsConstructor
public class ProdutoImagem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_codigo_santri", nullable = false)
    private Produto produto;

    @Column(name = "imagem_url", nullable = false, length = 1000)
    private String url;

    @Column(nullable = false)
    private int ordem;

    ProdutoImagem(Produto produto, String url, int ordem) {
        this.produto = produto;
        this.url = url;
        this.ordem = ordem;
    }

    void atualizarOrdem(int ordem) {
        this.ordem = ordem;
    }
}
