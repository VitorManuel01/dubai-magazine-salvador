package com.ecommerceproject.dubaimagazinesalvador.domain.vitrine.loja;

import java.util.ArrayList;
import java.util.List;

import com.ecommerceproject.dubaimagazinesalvador.domain.produto.Produto;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "produtos_vitrine_loja",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_produto_vitrine_loja_produto",
                columnNames = "produto_codigo_santri"
        )
)
@Getter
@NoArgsConstructor
public class ProdutoVitrineLoja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vitrine_loja_id", nullable = false)
    private VitrineLoja vitrineLoja;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_codigo_santri", nullable = false)
    private Produto produto;

    @Column(name = "rotulo_opcao", nullable = false, length = 100)
    private String rotuloOpcao;

    @Column(nullable = false)
    private int ordem;

    @ElementCollection
    @CollectionTable(
            name = "imagens_produto_vitrine_loja",
            joinColumns = @JoinColumn(name = "produto_vitrine_loja_id")
    )
    @OrderColumn(name = "ordem")
    @Column(name = "imagem_url", nullable = false, length = 1000)
    private List<String> imagens = new ArrayList<>();

    @OneToMany(
            mappedBy = "produtoVitrineLoja",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("ordem ASC, id ASC")
    private List<SecaoVitrineLoja> secoes = new ArrayList<>();

    public ProdutoVitrineLoja(
            Produto produto,
            String rotuloOpcao,
            int ordem,
            List<String> imagens
    ) {
        this.produto = produto;
        this.rotuloOpcao = rotuloOpcao;
        this.ordem = ordem;
        this.imagens.addAll(imagens);
    }

    void vincular(VitrineLoja vitrineLoja) {
        this.vitrineLoja = vitrineLoja;
    }

    public void adicionarSecao(SecaoVitrineLoja secao) {
        secao.vincular(this);
        secoes.add(secao);
    }
}

