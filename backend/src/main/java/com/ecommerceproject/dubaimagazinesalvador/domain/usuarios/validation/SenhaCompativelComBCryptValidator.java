package com.ecommerceproject.dubaimagazinesalvador.domain.usuarios.validation;

import java.nio.charset.StandardCharsets;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class SenhaCompativelComBCryptValidator
        implements ConstraintValidator<SenhaCompativelComBCrypt, String> {

    @Override
    public boolean isValid(String valor, ConstraintValidatorContext contexto) {
        return valor == null || valor.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}
