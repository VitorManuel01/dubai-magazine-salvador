package com.ecommerceproject.dubaimagazinesalvador.domain.usuarios;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity(name="administradores")
@Table(name="administradores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper=true)
public class Administrador extends Usuario implements IsAdmin{
    private boolean admin;
    private String nome;
    private String CPF;
    @Column(columnDefinition = "CHAR(1)")
    private String sexo;
    private LocalDate dataNascimento;
    private String CEP;
    private String endereco;
    private String bairro;
    private String telefone;
    
    @Override
    public boolean isAdmin() {
        return this.admin;
    }


}
