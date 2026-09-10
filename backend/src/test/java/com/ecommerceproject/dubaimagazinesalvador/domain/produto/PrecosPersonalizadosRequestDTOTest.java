package com.ecommerceproject.dubaimagazinesalvador.domain.produto;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import jakarta.validation.Validation;

class PrecosPersonalizadosRequestDTOTest {
    @Test void rejeitaValoresInvalidos() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            for (String valor : new String[]{"0", "-1", "1.001", "10000000000.00"}) {
                assertThat(validator.validate(new PrecosPersonalizadosRequestDTO(true,
                        new BigDecimal(valor), new BigDecimal(valor), 1))).isNotEmpty();
            }
            for (int parcelas : new int[]{0, -1, 37}) {
                assertThat(validator.validate(new PrecosPersonalizadosRequestDTO(true,
                        BigDecimal.TEN, BigDecimal.TEN, parcelas))).isNotEmpty();
            }
            assertThat(validator.validate(new PrecosPersonalizadosRequestDTO(true,
                    BigDecimal.TEN, BigDecimal.TEN, 36))).isEmpty();
        }
    }

    @Test void naoAtivaComCamposFaltandoENaoAlteraConfiguracaoComRequisicaoAntiga() {
        var produto = new Produto();
        assertThatThrownBy(() -> new PrecosPersonalizadosRequestDTO(true, BigDecimal.TEN, null, 10)
                .aplicar(produto)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThat(produto.isUsarPrecosPersonalizados()).isFalse();
        new PrecosPersonalizadosRequestDTO(true, BigDecimal.TEN, BigDecimal.TEN, 10).aplicar(produto);
        new PrecosPersonalizadosRequestDTO(null, null, null, null).aplicar(produto);
        assertThat(produto.isUsarPrecosPersonalizados()).isTrue();
    }
}
