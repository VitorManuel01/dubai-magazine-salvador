package com.ecommerceproject.dubaimagazinesalvador.domain.vitrine;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "depoimentos_home")
@Getter
@NoArgsConstructor
public class DepoimentoHome {

    @Id
    private Integer posicao;

    @Column(nullable = false, length = 80)
    private String nome;

    @Column(nullable = false, length = 300)
    private String texto;

    public DepoimentoHome(Integer posicao, String nome, String texto) {
        this.posicao = posicao;
        this.nome = nome;
        this.texto = texto;
    }

    public void atualizar(String nome, String texto) {
        this.nome = nome;
        this.texto = texto;
    }
}
