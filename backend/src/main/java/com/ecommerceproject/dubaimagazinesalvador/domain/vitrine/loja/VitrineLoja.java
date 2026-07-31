package com.ecommerceproject.dubaimagazinesalvador.domain.vitrine.loja;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "vitrines_loja")
@Getter
@NoArgsConstructor
public class VitrineLoja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private boolean ativo;

    @OneToMany(
            mappedBy = "vitrineLoja",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("ordem ASC, id ASC")
    private List<ProdutoVitrineLoja> opcoes = new ArrayList<>();

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    public VitrineLoja(boolean ativo) {
        this.ativo = ativo;
    }

    public void atualizarAtivo(boolean ativo) {
        this.ativo = ativo;
        this.atualizadoEm = LocalDateTime.now();
    }

    public void adicionarOpcao(ProdutoVitrineLoja opcao) {
        opcao.vincular(this);
        opcoes.add(opcao);
    }

    public void removerTodasOpcoes() {
        opcoes.clear();
    }

    @PrePersist
    private void antesDeCriar() {
        LocalDateTime agora = LocalDateTime.now();
        criadoEm = agora;
        atualizadoEm = agora;
    }

    @PreUpdate
    private void antesDeAtualizar() {
        atualizadoEm = LocalDateTime.now();
    }
}
