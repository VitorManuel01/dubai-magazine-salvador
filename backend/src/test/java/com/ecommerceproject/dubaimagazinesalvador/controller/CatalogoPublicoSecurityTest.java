package com.ecommerceproject.dubaimagazinesalvador.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.ecommerceproject.dubaimagazinesalvador.infra.security.SecurityConfigurations;
import com.ecommerceproject.dubaimagazinesalvador.infra.security.SecurityFilter;
import com.ecommerceproject.dubaimagazinesalvador.infra.security.TokenService;
import com.ecommerceproject.dubaimagazinesalvador.repositories.CategoriaRepository;
import com.ecommerceproject.dubaimagazinesalvador.repositories.ProdutoRepository;
import com.ecommerceproject.dubaimagazinesalvador.repositories.UsuarioRepository;
import com.ecommerceproject.dubaimagazinesalvador.services.produto.ApresentacaoProdutoService;

@WebMvcTest({ProdController.class, CategoriaController.class})
@Import({SecurityConfigurations.class, SecurityFilter.class})
class CatalogoPublicoSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProdutoRepository produtoRepository;

    @MockBean
    private CategoriaRepository categoriaRepository;

    @MockBean
    private ApresentacaoProdutoService apresentacaoProdutoService;

    @MockBean
    private TokenService tokenService;

    @MockBean
    private UsuarioRepository usuarioRepository;

    @Test
    void visitantePodeAcessarSomenteCatalogoPublico() throws Exception {
        when(produtoRepository.findCatalogoPorCategoria(
                isNull(),
                isNull(),
                eq(false),
                eq(false),
                any(Pageable.class)
        )).thenReturn(Page.empty());
        when(categoriaRepository.findByExibirNoSiteTrueOrderByCaminhoAsc())
                .thenReturn(List.of());

        mockMvc.perform(get("/produto"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/categoria"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/admin/produtos"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/categorias"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "FUNCIONARIO")
    void funcionarioNaoPodeConsultarDadosAdministrativosDoCatalogo() throws Exception {
        mockMvc.perform(get("/admin/produtos"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/categorias"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void administradorPodeConsultarDadosAdministrativosDoCatalogo() throws Exception {
        when(produtoRepository.findCatalogoPorCategoria(
                isNull(),
                isNull(),
                eq(true),
                eq(false),
                any(Pageable.class)
        )).thenReturn(Page.empty());
        when(categoriaRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/admin/produtos"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/admin/categorias"))
                .andExpect(status().isOk());
    }
}
