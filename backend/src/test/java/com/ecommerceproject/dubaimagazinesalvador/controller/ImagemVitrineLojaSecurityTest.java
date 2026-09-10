package com.ecommerceproject.dubaimagazinesalvador.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import com.ecommerceproject.dubaimagazinesalvador.infra.security.SecurityConfigurations;
import com.ecommerceproject.dubaimagazinesalvador.infra.security.SecurityFilter;
import com.ecommerceproject.dubaimagazinesalvador.infra.security.TokenService;
import com.ecommerceproject.dubaimagazinesalvador.repositories.UsuarioRepository;
import com.ecommerceproject.dubaimagazinesalvador.services.produto.ArmazenamentoImagemProdutoService;

@WebMvcTest(ImagemVitrineLojaController.class)
@Import({SecurityConfigurations.class, SecurityFilter.class})
@ActiveProfiles("test")
class ImagemVitrineLojaSecurityTest extends ProtecoesWebTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ArmazenamentoImagemProdutoService armazenamentoImagem;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @Test
    void funcionarioNaoPodeEnviarImagem() throws Exception {
        mockMvc.perform(multipart("/admin/vitrine-loja/imagens")
                        .file(imagem())
                        .with(user("funcionario").roles("FUNCIONARIO")))
                .andExpect(status().isForbidden());
    }

    @Test
    void administradorPodeEnviarImagem() throws Exception {
        String identificador = "123e4567-e89b-12d3-a456-426614174000.webp";
        when(armazenamentoImagem.salvar(any()))
                .thenReturn("/uploads/produtos/" + identificador);

        mockMvc.perform(multipart("/admin/vitrine-loja/imagens")
                        .file(imagem())
                        .with(user("administrador").roles("ADMIN")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url")
                        .value("/catalogo/imagens/" + identificador));
    }

    private MockMultipartFile imagem() {
        return new MockMultipartFile(
                "imagem",
                "produto.webp",
                "image/webp",
                new byte[] { 'R', 'I', 'F', 'F' }
        );
    }
}
