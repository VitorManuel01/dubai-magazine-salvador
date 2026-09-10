package com.ecommerceproject.dubaimagazinesalvador.domain.usuarios;

import java.util.UUID;

public record FuncionarioResponseDTO(
        UUID id,
        String codigoSantri,
        String nomeFuncionario,
        Role funcao,
        boolean ativo
) {
    
    public FuncionarioResponseDTO(Funcionario funcionario){
        this(
                funcionario.getId(),
                funcionario.getCodigoSantri(),
                funcionario.getNomeFuncionario(),
                funcionario.getFuncao(),
                funcionario.isAtivo()
        );
    }

}
