package com.ecommerceproject.dubaimagazinesalvador.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ecommerceproject.dubaimagazinesalvador.domain.produto.VisibilidadeProdutosResponseDTO;
import com.ecommerceproject.dubaimagazinesalvador.infra.security.SecurityConfigurations;
import com.ecommerceproject.dubaimagazinesalvador.infra.security.SecurityFilter;
import com.ecommerceproject.dubaimagazinesalvador.infra.security.TokenService;
import com.ecommerceproject.dubaimagazinesalvador.repositories.UsuarioRepository;
import com.ecommerceproject.dubaimagazinesalvador.services.produto.VisibilidadeProdutosService;

@WebMvcTest(VisibilidadeProdutosController.class)
@Import({SecurityConfigurations.class, SecurityFilter.class})
@ActiveProfiles("test")
class VisibilidadeProdutosSecurityTest extends ProtecoesWebTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VisibilidadeProdutosService service;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @Test
    void somenteAdministradorPodeAlterarVisibilidadeEmLote() throws Exception {
        String corpo = "{\"codigosSantri\":[\"100\",\"200\"],\"exibirNoSite\":false}";
        when(service.alterarVisibilidade(any()))
                .thenReturn(new VisibilidadeProdutosResponseDTO(2, 2, false));

        mockMvc.perform(put("/admin/produtos/visibilidade")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/admin/produtos/visibilidade")
                        .with(user("funcionario").roles("FUNCIONARIO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/admin/produtos/visibilidade")
                        .with(user("administrador").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.produtosAlterados").value(2))
                .andExpect(jsonPath("$.exibirNoSite").value(false));
    }
}
