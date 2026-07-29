package com.ecommerceproject.dubaimagazinesalvador.domain.vitrine;

import com.ecommerceproject.dubaimagazinesalvador.domain.categoria.Categoria;

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
@Table(name = "vitrines_home")
@Getter
@NoArgsConstructor
public class VitrineHome {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_codigo", nullable = false)
    private Categoria categoria;

    @Column(nullable = false, length = 180)
    private String titulo;

    @Column(nullable = false, length = 1000)
    private String descricao;

    @Column(nullable = false)
    private int ordem;

    @Column(nullable = false)
    private boolean ativo;

    public VitrineHome(
            Categoria categoria,
            String titulo,
            String descricao,
            int ordem,
            boolean ativo
    ) {
        atualizar(categoria, titulo, descricao, ordem, ativo);
    }

    public void atualizar(
            Categoria categoria,
            String titulo,
            String descricao,
            int ordem,
            boolean ativo
    ) {
        this.categoria = categoria;
        this.titulo = titulo;
        this.descricao = descricao;
        this.ordem = ordem;
        this.ativo = ativo;
    }
}
