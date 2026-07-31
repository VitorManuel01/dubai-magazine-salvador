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

@Entity(name = "clientes")
@Table(name = "clientes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Cliente extends Usuario {

    private String nomeCliente;

    private String CPF;

    @Column(columnDefinition = "CHAR(1)")
    private String sexo;

    private LocalDate dataNascimento;

    private String CEP;

    private String bairro;

    private String telefone;


    public Cliente( String senha,boolean admin, String nomeCliente,  
                     String CPF, String sexo, LocalDate dataNascimento,String CEP,  String bairro, 
                       String telefone) {
    this.nomeCliente = nomeCliente;
    this.CPF = CPF;
    this.sexo = sexo;
    this.dataNascimento = dataNascimento;
    this.CEP = CEP;
    this.bairro = bairro;
    this.telefone = telefone;
}

}
