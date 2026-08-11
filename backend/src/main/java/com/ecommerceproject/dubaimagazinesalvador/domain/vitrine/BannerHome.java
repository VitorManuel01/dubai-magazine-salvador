package com.ecommerceproject.dubaimagazinesalvador.domain.vitrine;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "banners_home")
@Getter
@NoArgsConstructor
public class BannerHome {

    @Id
    private Integer posicao;

    @Column(name = "imagem_url", length = 500)
    private String imagemUrl;

    public BannerHome(Integer posicao, String imagemUrl) {
        this.posicao = posicao;
        this.imagemUrl = imagemUrl;
    }

    public void atualizarImagem(String imagemUrl) {
        this.imagemUrl = imagemUrl;
    }
}
