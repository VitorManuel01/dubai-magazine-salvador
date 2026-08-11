package com.ecommerceproject.dubaimagazinesalvador.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ecommerceproject.dubaimagazinesalvador.domain.vitrine.BannerHomeResponseDTO;
import com.ecommerceproject.dubaimagazinesalvador.infra.security.SecurityConfigurations;
import com.ecommerceproject.dubaimagazinesalvador.infra.security.SecurityFilter;
import com.ecommerceproject.dubaimagazinesalvador.infra.security.TokenService;
import com.ecommerceproject.dubaimagazinesalvador.repositories.UsuarioRepository;
import com.ecommerceproject.dubaimagazinesalvador.services.vitrine.BannerHomeService;

@WebMvcTest(BannerHomeController.class)
@Import({SecurityConfigurations.class, SecurityFilter.class})
class BannerHomeSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BannerHomeService bannerHomeService;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @Test
    void consultaDosBannersEhPublica() throws Exception {
        when(bannerHomeService.listar()).thenReturn(List.of(
                new BannerHomeResponseDTO(1, null),
                new BannerHomeResponseDTO(2, null),
                new BannerHomeResponseDTO(3, null)
        ));

        mockMvc.perform(get("/banners-home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].posicao").value(1));
    }

    @Test
    void funcionarioNaoPodeTrocarBanner() throws Exception {
        mockMvc.perform(requisicaoAtualizacao()
                        .with(user("funcionario").roles("FUNCIONARIO")))
                .andExpect(status().isForbidden());
    }

    @Test
    void administradorPodeTrocarBanner() throws Exception {
        String url = "/catalogo/imagens/22222222-2222-4222-8222-222222222222.webp";
        when(bannerHomeService.atualizar(eq(1), any()))
                .thenReturn(new BannerHomeResponseDTO(1, url));

        mockMvc.perform(requisicaoAtualizacao()
                        .with(user("administrador").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imagemUrl").value(url));
    }

    private org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder
            requisicaoAtualizacao() {
        MockMultipartFile imagem = new MockMultipartFile(
                "imagem",
                "banner.webp",
                "image/webp",
                new byte[] { 'R', 'I', 'F', 'F' }
        );
        return multipart("/admin/banners-home/1")
                .file(imagem)
                .with(request -> {
                    request.setMethod("PUT");
                    return request;
                });
    }
}
