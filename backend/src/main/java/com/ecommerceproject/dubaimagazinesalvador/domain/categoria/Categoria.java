package com.ecommerceproject.dubaimagazinesalvador.domain.categoria;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "categorias")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "codigo")
public class Categoria {

    @Id
    @Column(length = 64)
    private String codigo;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private int nivel;

    @Column(nullable = false, length = 1000)
    private String caminho;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_pai_codigo")
    private Categoria categoriaPai;

    @Column(name = "exibir_no_site", nullable = false)
    private boolean exibirNoSite;

    public void atualizarDadosImportados(String nome, int nivel, String caminho, Categoria categoriaPai) {
        this.nome = nome;
        this.nivel = nivel;
        this.caminho = caminho;
        this.categoriaPai = categoriaPai;
    }
}
