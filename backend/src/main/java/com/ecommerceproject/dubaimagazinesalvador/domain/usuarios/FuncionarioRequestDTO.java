package com.ecommerceproject.dubaimagazinesalvador.domain.usuarios;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record FuncionarioRequestDTO(
        @NotBlank
        @Size(min = 4, max = 50)
        @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "O login deve conter apenas letras, números, ponto, hífen ou sublinhado")
        String login,

        @NotBlank
        @Email
        @Size(max = 254)
        String email,

        @NotBlank
        @Size(min = 12, max = 128)
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$",
                message = "A senha deve conter letra maiúscula, minúscula, número e caractere especial"
        )
        String senha,

        @NotBlank
        @Size(min = 2, max = 120)
        @Pattern(regexp = "^[\\p{L} .'-]+$", message = "O nome contém caracteres inválidos")
        String nomeFuncionario,

        @NotBlank
        @Pattern(regexp = "^\\d{11}$", message = "O CPF deve conter 11 números")
        String CPF,

        @Pattern(regexp = "^$|^[MFO]$", message = "Sexo inválido")
        String sexo,

        @Past(message = "A data de nascimento deve estar no passado")
        LocalDate dataNascimento,

        @Pattern(regexp = "^$|^\\d{8}$", message = "O CEP deve conter 8 números")
        String CEP,

        @Size(max = 120)
        String bairro,

        @Pattern(regexp = "^$|^\\d{10,11}$", message = "O telefone deve conter 10 ou 11 números")
        String telefone
) {
}
