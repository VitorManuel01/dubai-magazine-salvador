package com.ecommerceproject.dubaimagazinesalvador.domain.usuarios;

import java.time.LocalDate;
import java.util.UUID;

public record FuncionarioResponseDTO(UUID id, String codigoSantri, Role funcao, String nomeFuncionario, String CPF, String sexo, LocalDate dataNascimento, String CEP, String bairro, String telefone) {
    
    public FuncionarioResponseDTO(Funcionario funcionario){
        this(funcionario.getId(), funcionario.getCodigoSantri(), funcionario.getFuncao(), funcionario.getNomeFuncionario(), funcionario.getCPF(), funcionario.getSexo(), funcionario.getDataNascimento(), funcionario.getCEP(), funcionario.getBairro(), funcionario.getTelefone());
    }

}
