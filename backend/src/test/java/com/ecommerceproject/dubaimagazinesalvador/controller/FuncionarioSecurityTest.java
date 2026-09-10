package com.ecommerceproject.dubaimagazinesalvador.controller;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasKey;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
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
@ActiveProfiles("test")
class FuncionarioSecurityTest extends ProtecoesWebTestSupport {

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

    @MockitoBean
    private FuncionarioRepository funcionarioRepository;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private TokenService tokenService;

    @Test
    void visitanteNaoPodeCadastrarFuncionario() throws Exception {
        mockMvc.perform(post("/funcionario")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(FUNCIONARIO_VALIDO))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void funcionarioNaoPodeCadastrarOutroFuncionario() throws Exception {
        mockMvc.perform(post("/funcionario")
                        .with(user("funcionario").roles("FUNCIONARIO"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(FUNCIONARIO_VALIDO))
                .andExpect(status().isForbidden());
    }

    @Test
    void administradorCadastraFuncionarioComSenhaCriptografadaEFuncaoForcada() throws Exception {
        when(usuarioRepository.existsByCodigoSantriIgnoreCase("FUN-001"))
                .thenReturn(false);
        when(passwordEncoder.encode("SenhaForte@2026"))
                .thenReturn("$2a$12$hash-seguro");
        when(funcionarioRepository.save(any(Funcionario.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        mockMvc.perform(post("/funcionario")
                        .with(user("administrador").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(FUNCIONARIO_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", not(hasKey("senha"))))
                .andExpect(jsonPath("$", not(hasKey("CPF"))))
                .andExpect(jsonPath("$", not(hasKey("dataNascimento"))))
                .andExpect(jsonPath("$", not(hasKey("CEP"))))
                .andExpect(jsonPath("$", not(hasKey("bairro"))))
                .andExpect(jsonPath("$", not(hasKey("telefone"))))
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
    void senhaFracaERejeitadaAntesDeSalvar() throws Exception {
        mockMvc.perform(post("/funcionario")
                        .with(user("administrador").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(FUNCIONARIO_VALIDO.replace("SenhaForte@2026", "123")))
                .andExpect(status().isBadRequest());

        verify(passwordEncoder, never()).encode(any());
        verify(funcionarioRepository, never()).save(any());
    }

    @Test
    void funcionarioNaoPodeListarDadosDeOutrosFuncionarios() throws Exception {
        mockMvc.perform(get("/funcionario")
                        .with(user("funcionario").roles("FUNCIONARIO")))
                .andExpect(status().isForbidden());
    }

    @Test
    void administradorListaSomenteResumoDosFuncionarios() throws Exception {
        UUID id = UUID.randomUUID();
        Funcionario funcionario = funcionario(id, true);
        when(funcionarioRepository.findAll()).thenReturn(List.of(funcionario));

        mockMvc.perform(get("/funcionario")
                        .with(user("administrador").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()))
                .andExpect(jsonPath("$[0].codigoSantri").value("FUN-001"))
                .andExpect(jsonPath("$[0].nomeFuncionario").value("Funcionário Teste"))
                .andExpect(jsonPath("$[0].funcao").value("funcionario"))
                .andExpect(jsonPath("$[0].ativo").value(true))
                .andExpect(jsonPath("$[0]", not(hasKey("CPF"))))
                .andExpect(jsonPath("$[0]", not(hasKey("sexo"))))
                .andExpect(jsonPath("$[0]", not(hasKey("dataNascimento"))))
                .andExpect(jsonPath("$[0]", not(hasKey("CEP"))))
                .andExpect(jsonPath("$[0]", not(hasKey("bairro"))))
                .andExpect(jsonPath("$[0]", not(hasKey("telefone"))));
    }

    @Test
    void administradorDesativaFuncionarioERevogaTokensEmitidos() throws Exception {
        UUID id = UUID.randomUUID();
        Funcionario funcionario = funcionario(id, true);
        when(usuarioRepository.buscarPorIdParaAtualizacao(id)).thenReturn(Optional.of(funcionario));
        when(funcionarioRepository.saveAndFlush(funcionario)).thenReturn(funcionario);

        mockMvc.perform(put("/funcionario/{id}/status", id)
                        .with(user("administrador").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ativo\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.ativo").value(false))
                .andExpect(jsonPath("$", not(hasKey("CPF"))))
                .andExpect(jsonPath("$", not(hasKey("dataNascimento"))))
                .andExpect(jsonPath("$", not(hasKey("telefone"))));

        org.junit.jupiter.api.Assertions.assertFalse(funcionario.isAtivo());
        org.junit.jupiter.api.Assertions.assertEquals(1, funcionario.getVersaoToken());
        verify(funcionarioRepository).saveAndFlush(funcionario);
    }

    @Test
    void funcionarioNaoPodeAlterarStatusDeOutroFuncionario() throws Exception {
        mockMvc.perform(put("/funcionario/{id}/status", UUID.randomUUID())
                        .with(user("funcionario").roles("FUNCIONARIO"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ativo\":false}"))
                .andExpect(status().isForbidden());

        verify(funcionarioRepository, never()).saveAndFlush(any());
    }

    private Funcionario funcionario(UUID id, boolean ativo) {
        Funcionario funcionario = new Funcionario();
        funcionario.setId(id);
        funcionario.setCodigoSantri("FUN-001");
        funcionario.setNomeFuncionario("Funcionário Teste");
        funcionario.setFuncao(Role.ROLE_FUNCIONARIO);
        funcionario.setAtivo(ativo);
        return funcionario;
    }
}
