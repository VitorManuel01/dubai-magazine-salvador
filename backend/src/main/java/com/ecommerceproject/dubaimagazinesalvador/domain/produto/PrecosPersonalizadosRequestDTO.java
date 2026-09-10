package com.ecommerceproject.dubaimagazinesalvador.domain.produto;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public record PrecosPersonalizadosRequestDTO(
        Boolean usarPrecosPersonalizados,
        @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal precoAVista,
        @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal precoCartaoParc,
        @Min(1) @Max(36) Integer maxParcelamento
) {
    public void aplicar(Produto produto) {
        // Clientes antigos, que não enviam estes campos, preservam a configuração.
        if (usarPrecosPersonalizados == null) {
            if (precoAVista != null || precoCartaoParc != null || maxParcelamento != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o modo de preço.");
            }
            return;
        }
        if (usarPrecosPersonalizados && (precoAVista == null || precoCartaoParc == null || maxParcelamento == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe o preço à vista, o total no cartão e o máximo de parcelas.");
        }
        produto.setUsarPrecosPersonalizados(usarPrecosPersonalizados);
        // Desligar sem enviar os valores não apaga os preços manuais já cadastrados.
        if (precoAVista != null) produto.setPrecoAVista(precoAVista);
        if (precoCartaoParc != null) produto.setPrecoCartaoParc(precoCartaoParc);
        if (maxParcelamento != null) produto.setMaxParcelamento(maxParcelamento);
    }
}
