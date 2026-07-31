package com.ecommerceproject.dubaimagazinesalvador.domain.vitrine.loja;

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
@Table(name = "secoes_produto_vitrine_loja")
@Getter
@NoArgsConstructor
public class SecaoVitrineLoja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_vitrine_loja_id", nullable = false)
    private ProdutoVitrineLoja produtoVitrineLoja;

    @Column(nullable = false, length = 180)
    private String titulo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String conteudo;

    @Column(nullable = false)
    private int ordem;

    public SecaoVitrineLoja(String titulo, String conteudo, int ordem) {
        this.titulo = titulo;
        this.conteudo = conteudo;
        this.ordem = ordem;
    }

    void vincular(ProdutoVitrineLoja produtoVitrineLoja) {
        this.produtoVitrineLoja = produtoVitrineLoja;
    }
}

