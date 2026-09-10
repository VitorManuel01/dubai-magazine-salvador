package com.ecommerceproject.dubaimagazinesalvador.domain.usuarios;

import com.ecommerceproject.dubaimagazinesalvador.domain.usuarios.validation.SenhaCompativelComBCrypt;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthenticationDTO(
        @NotBlank @Size(max = 50) String codigoSantri,
        @NotBlank @Size(max = 72) @SenhaCompativelComBCrypt String senha
) {

}
