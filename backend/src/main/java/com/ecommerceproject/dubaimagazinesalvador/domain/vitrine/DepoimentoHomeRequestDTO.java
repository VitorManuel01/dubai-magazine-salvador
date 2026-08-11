package com.ecommerceproject.dubaimagazinesalvador.domain.vitrine;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DepoimentoHomeRequestDTO(
        @Min(1) @Max(3) int posicao,
        @NotBlank @Size(max = 80) String nome,
        @NotBlank @Size(max = 300) String texto
) {
}
