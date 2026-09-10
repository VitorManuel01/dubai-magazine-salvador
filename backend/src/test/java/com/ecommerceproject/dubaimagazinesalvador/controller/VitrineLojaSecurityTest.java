package com.ecommerceproject.dubaimagazinesalvador.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.ecommerceproject.dubaimagazinesalvador.domain.vitrine.loja.VitrineLojaResponseDTO;
import com.ecommerceproject.dubaimagazinesalvador.infra.security.SecurityConfigurations;
import com.ecommerceproject.dubaimagazinesalvador.infra.security.SecurityFilter;
import com.ecommerceproject.dubaimagazinesalvador.infra.security.TokenService;
import com.ecommerceproject.dubaimagazinesalvador.repositories.UsuarioRepository;
import com.ecommerceproject.dubaimagazinesalvador.services.vitrine.VitrineLojaService;

@WebMvcTest(VitrineLojaController.class)
@Import({SecurityConfigurations.class, SecurityFilter.class})
@ActiveProfiles("test")
class VitrineLojaSecurityTest extends ProtecoesWebTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VitrineLojaService vitrineLojaService;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @Test
    void funcionarioPodePesquisarVitrinesAtivas() throws Exception {
        when(vitrineLojaService.pesquisar(isNull(), eq(0), eq(12), eq(true)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/vitrine-loja")
                        .with(user("funcionario").roles("FUNCIONARIO")))
                .andExpect(status().isOk());

        verify(vitrineLojaService).pesquisar(null, 0, 12, true);
    }

    @Test
    void clienteNaoPodeAcessarVitrineInterna() throws Exception {
        mockMvc.perform(get("/vitrine-loja")
                        .with(user("cliente").roles("CLIENTE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void funcionarioNaoPodeCriarVitrine() throws Exception {
        mockMvc.perform(post("/admin/vitrine-loja")
                        .with(user("funcionario").roles("FUNCIONARIO"))
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
                        .with(user("administrador").roles("ADMIN"))
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
    void administradorPodePesquisarProdutosVisiveisEOcultosParaVitrine() throws Exception {
        when(vitrineLojaService.pesquisarProdutosParaAdministracao(
                isNull(),
                eq(0),
                eq(60)
        )).thenReturn(Page.empty());

        mockMvc.perform(get("/admin/vitrine-loja/produtos")
                        .with(user("administrador").roles("ADMIN")))
                .andExpect(status().isOk());

        verify(vitrineLojaService).pesquisarProdutosParaAdministracao(
                null,
                0,
                60
        );
    }
}
