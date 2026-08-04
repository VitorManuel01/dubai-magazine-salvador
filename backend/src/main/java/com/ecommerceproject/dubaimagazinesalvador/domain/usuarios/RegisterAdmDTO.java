package com.ecommerceproject.dubaimagazinesalvador.domain.usuarios;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterAdmDTO(
        @NotBlank
        @Size(max = 50)
        @Pattern(
                regexp = "^[A-Za-z0-9._-]+$",
                message = "O código Santri contém caracteres inválidos"
        )
        String codigoSantri,

        @NotBlank
        @Size(min = 12, max = 128)
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$",
                message = "A senha deve conter letra maiúscula, minúscula, número e caractere especial"
        )
        String senha,

        @NotBlank @Size(min = 2, max = 120) String nome,
        @NotBlank @Pattern(regexp = "^\\d{11}$") String CPF,
        @Pattern(regexp = "^$|^[MFO]$") String sexo,
        @Past LocalDate dataNascimento,
        @Pattern(regexp = "^$|^\\d{8}$") String CEP,
        @Size(max = 160) String endereco,
        @Size(max = 120) String bairro,
        @Pattern(regexp = "^$|^\\d{10,11}$") String telefone
) {
}
