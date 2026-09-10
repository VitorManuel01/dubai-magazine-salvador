package com.ecommerceproject.dubaimagazinesalvador.controller;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ecommerceproject.dubaimagazinesalvador.domain.vitrine.DepoimentoHomeResponseDTO;
import com.ecommerceproject.dubaimagazinesalvador.infra.security.SecurityConfigurations;
import com.ecommerceproject.dubaimagazinesalvador.infra.security.SecurityFilter;
import com.ecommerceproject.dubaimagazinesalvador.infra.security.TokenService;
import com.ecommerceproject.dubaimagazinesalvador.repositories.UsuarioRepository;
import com.ecommerceproject.dubaimagazinesalvador.services.vitrine.DepoimentoHomeService;

@WebMvcTest(DepoimentoHomeController.class)
@Import({SecurityConfigurations.class, SecurityFilter.class})
@ActiveProfiles("test")
class DepoimentoHomeSecurityTest extends ProtecoesWebTestSupport {

    private static final String CORPO_VALIDO = """
            {
              "depoimentos": [
                {"posicao": 1, "nome": "Ana", "texto": "Excelente."},
                {"posicao": 2, "nome": "Bruno", "texto": "Muito bom."},
                {"posicao": 3, "nome": "Carla", "texto": "Recomendo."}
              ]
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DepoimentoHomeService service;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @Test
    void consultaDosDepoimentosEhPublica() throws Exception {
        when(service.listar()).thenReturn(resposta());

        mockMvc.perform(get("/depoimentos-home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Ana"));
    }

    @Test
    void funcionarioNaoPodeEditarDepoimentos() throws Exception {
        mockMvc.perform(put("/admin/depoimentos-home")
                        .with(user("funcionario").roles("FUNCIONARIO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CORPO_VALIDO))
                .andExpect(status().isForbidden());
    }

    @Test
    void administradorPodeEditarDepoimentos() throws Exception {
        when(service.atualizar(anyList())).thenReturn(resposta());

        mockMvc.perform(put("/admin/depoimentos-home")
                        .with(user("administrador").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CORPO_VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[2].texto").value("Recomendo."));
    }

    private List<DepoimentoHomeResponseDTO> resposta() {
        return List.of(
                new DepoimentoHomeResponseDTO(1, "Ana", "Excelente."),
                new DepoimentoHomeResponseDTO(2, "Bruno", "Muito bom."),
                new DepoimentoHomeResponseDTO(3, "Carla", "Recomendo.")
        );
    }
}
