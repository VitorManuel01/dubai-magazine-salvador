package com.ecommerceproject.dubaimagazinesalvador.domain.usuarios.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/** Impede truncamento silencioso além do limite de 72 bytes do BCrypt. */
@Documented
@Constraint(validatedBy = SenhaCompativelComBCryptValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface SenhaCompativelComBCrypt {

    String message() default "A senha deve possuir no máximo 72 bytes em UTF-8";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
