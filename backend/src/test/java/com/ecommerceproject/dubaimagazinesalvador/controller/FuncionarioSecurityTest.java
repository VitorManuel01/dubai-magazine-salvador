package com.ecommerceproject.dubaimagazinesalvador.controller;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasKey;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.ecommerceproject.dubaimagazinesalvador.domain.usuarios.Funcionario;
import com.ecommerceproject.dubaimagazinesalvador.domain.usuarios.Role;
import com.ecommerceproject.dubaimagazinesalvador.infra.security.SecurityConfigurations;
import com.ecommerceproject.dubaimagazinesalvador.infra.security.SecurityFilter;
import com.ecommerceproject.dubaimagazinesalvador.infra.security.TokenService;
import com.ecommerceproject.dubaimagazinesalvador.repositories.FuncionarioRepository;
import com.ecommerceproject.dubaimagazinesalvador.repositories.UsuarioRepository;

@WebMvcTest(FunController.class)
@Import({SecurityConfigurations.class, SecurityFilter.class})
class FuncionarioSecurityTest {

    private static final String FUNCIONARIO_VALIDO = """
            {
              "codigoSantri": "FUN-001",
              "senha": "SenhaForte@2026",
              "funcao": "admin",
              "nomeFuncionario": "Funcionário Teste",
              "CPF": "10112019501",
              "sexo": "M",
              "dataNascimento": "2000-01-01",
              "CEP": "40000000",
              "bairro": "Centro",
              "telefone": "71999999999"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FuncionarioRepository funcionarioRepository;

    @MockBean
    private UsuarioRepository usuarioRepository;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private TokenService tokenService;

    @Test
    void visitanteNaoPodeCadastrarFuncionario() throws Exception {
        mockMvc.perform(post("/funcionario")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(FUNCIONARIO_VALIDO))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "FUNCIONARIO")
    void funcionarioNaoPodeCadastrarOutroFuncionario() throws Exception {
        mockMvc.perform(post("/funcionario")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(FUNCIONARIO_VALIDO))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void administradorCadastraFuncionarioComSenhaCriptografadaEFuncaoForcada() throws Exception {
        when(usuarioRepository.existsByCodigoSantriIgnoreCase("FUN-001"))
                .thenReturn(false);
        when(passwordEncoder.encode("SenhaForte@2026"))
                .thenReturn("$2a$12$hash-seguro");
        when(funcionarioRepository.save(any(Funcionario.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        mockMvc.perform(post("/funcionario")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(FUNCIONARIO_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", not(hasKey("senha"))))
                .andExpect(jsonPath("$.funcao").value("funcionario"));

        ArgumentCaptor<Funcionario> captor = ArgumentCaptor.forClass(Funcionario.class);
        verify(funcionarioRepository).save(captor.capture());
        Funcionario salvo = captor.getValue();

        org.junit.jupiter.api.Assertions.assertEquals(
                "$2a$12$hash-seguro",
                salvo.getSenha()
        );
        org.junit.jupiter.api.Assertions.assertEquals(
                Role.ROLE_FUNCIONARIO,
                salvo.getFuncao()
        );
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void senhaFracaERejeitadaAntesDeSalvar() throws Exception {
        mockMvc.perform(post("/funcionario")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(FUNCIONARIO_VALIDO.replace("SenhaForte@2026", "123")))
                .andExpect(status().isBadRequest());

        verify(passwordEncoder, never()).encode(any());
        verify(funcionarioRepository, never()).save(any());
    }

    @Test
    @WithMockUser(roles = "FUNCIONARIO")
    void funcionarioNaoPodeListarDadosDeOutrosFuncionarios() throws Exception {
        mockMvc.perform(get("/funcionario"))
                .andExpect(status().isForbidden());
    }
}
