package com.ecommerceproject.dubaimagazinesalvador.domain.vitrine;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AtualizacaoDepoimentosHomeRequestDTO(
        @NotNull @Size(min = 3, max = 3)
        List<@Valid DepoimentoHomeRequestDTO> depoimentos
) {
}
