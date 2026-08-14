package com.ecommerceproject.dubaimagazinesalvador.domain.produto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record VisibilidadeProdutosRequestDTO(
        @NotEmpty(message = "Selecione ao menos um produto.")
        @Size(max = 200, message = "Selecione no máximo 200 produtos por operação.")
        List<String> codigosSantri,

        @NotNull(message = "Informe se os produtos devem ser exibidos ou ocultados.")
        Boolean exibirNoSite
) {
}
