package com.ecommerceproject.dubaimagazinesalvador.domain.usuarios;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthenticationDTO(
        @NotBlank @Size(max = 254) String emailOrLogin,
        @NotBlank @Size(max = 128) String senha
) {

}
