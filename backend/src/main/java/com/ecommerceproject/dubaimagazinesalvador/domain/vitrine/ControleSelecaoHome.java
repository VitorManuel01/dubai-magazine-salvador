package com.ecommerceproject.dubaimagazinesalvador.domain.vitrine;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "controle_selecao_home")
@Getter
@NoArgsConstructor
public class ControleSelecaoHome {

    @Id
    private Integer id;
}
