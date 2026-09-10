package com.ecommerceproject.dubaimagazinesalvador.domain.usuarios;

import jakarta.validation.constraints.NotNull;

public record AtualizacaoStatusUsuarioDTO(@NotNull Boolean ativo) {
}
