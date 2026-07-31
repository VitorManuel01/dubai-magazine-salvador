package com.ecommerceproject.dubaimagazinesalvador.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.ecommerceproject.dubaimagazinesalvador.domain.vitrine.loja.VitrineLojaResponseDTO;
import com.ecommerceproject.dubaimagazinesalvador.infra.security.SecurityConfigurations;
import com.ecommerceproject.dubaimagazinesalvador.infra.security.SecurityFilter;
import com.ecommerceproject.dubaimagazinesalvador.infra.security.TokenService;
import com.ecommerceproject.dubaimagazinesalvador.repositories.UsuarioRepository;
import com.ecommerceproject.dubaimagazinesalvador.services.vitrine.VitrineLojaService;

@WebMvcTest(VitrineLojaController.class)
@Import({SecurityConfigurations.class, SecurityFilter.class})
class VitrineLojaSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VitrineLojaService vitrineLojaService;

    @MockBean
    private TokenService tokenService;

    @MockBean
    private UsuarioRepository usuarioRepository;

    @Test
    @WithMockUser(roles = "FUNCIONARIO")
    void funcionarioPodePesquisarVitrinesAtivas() throws Exception {
        when(vitrineLojaService.pesquisar(isNull(), eq(0), eq(12), eq(true)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/vitrine-loja"))
                .andExpect(status().isOk());

        verify(vitrineLojaService).pesquisar(null, 0, 12, true);
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void clienteNaoPodeAcessarVitrineInterna() throws Exception {
        mockMvc.perform(get("/vitrine-loja"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "FUNCIONARIO")
    void funcionarioNaoPodeCriarVitrine() throws Exception {
        mockMvc.perform(post("/admin/vitrine-loja")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ativo": true,
                                  "opcoes": []
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void administradorPodeCriarVitrine() throws Exception {
        VitrineLojaResponseDTO resposta = new VitrineLojaResponseDTO(
                1L,
                true,
                List.of(),
                null,
                null
        );
        when(vitrineLojaService.criar(any())).thenReturn(resposta);

        mockMvc.perform(post("/admin/vitrine-loja")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ativo": true,
                                  "opcoes": []
                                }
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void administradorPodePesquisarProdutosVisiveisEOcultosParaVitrine() throws Exception {
        when(vitrineLojaService.pesquisarProdutosParaAdministracao(
                isNull(),
                eq(0),
                eq(60)
        )).thenReturn(Page.empty());

        mockMvc.perform(get("/admin/vitrine-loja/produtos"))
                .andExpect(status().isOk());

        verify(vitrineLojaService).pesquisarProdutosParaAdministracao(
                null,
                0,
                60
        );
    }
}
