package com.ecommerceproject.dubaimagazinesalvador.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ecommerceproject.dubaimagazinesalvador.domain.importacao.ImportacaoEstoqueResponseDTO;
import com.ecommerceproject.dubaimagazinesalvador.infra.security.SecurityConfigurations;
import com.ecommerceproject.dubaimagazinesalvador.infra.security.SecurityFilter;
import com.ecommerceproject.dubaimagazinesalvador.infra.security.TokenService;
import com.ecommerceproject.dubaimagazinesalvador.repositories.UsuarioRepository;
import com.ecommerceproject.dubaimagazinesalvador.services.importacao.ControleImportacaoProdutosService;
import com.ecommerceproject.dubaimagazinesalvador.services.importacao.ImportacaoEstoqueService;
import com.ecommerceproject.dubaimagazinesalvador.services.importacao.ImportacaoProdutosService;
import com.ecommerceproject.dubaimagazinesalvador.services.importacao.ImportacaoPromocoesService;

@WebMvcTest(ImportacaoProdutosController.class)
@Import({
        SecurityConfigurations.class,
        SecurityFilter.class,
        ControleImportacaoProdutosService.class
})
@ActiveProfiles("test")
class ImportacaoEstoqueSecurityTest extends ProtecoesWebTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ImportacaoProdutosService importacaoProdutosService;

    @MockitoBean
    private ImportacaoPromocoesService importacaoPromocoesService;

    @MockitoBean
    private ImportacaoEstoqueService importacaoEstoqueService;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @Test
    void somenteAdministradorPodeImportarInventario() throws Exception {
        when(importacaoEstoqueService.importar(any())).thenReturn(
                new ImportacaoEstoqueResponseDTO(
                        "inventario.ods",
                        86_919,
                        2_000,
                        84_919,
                        LocalDateTime.of(2026, 8, 14, 12, 0),
                        2_500
                )
        );

        mockMvc.perform(multipart("/admin/importacoes/estoque")
                        .file(arquivo()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(multipart("/admin/importacoes/estoque")
                        .file(arquivo())
                        .with(user("funcionario").roles("FUNCIONARIO")))
                .andExpect(status().isForbidden());
        mockMvc.perform(multipart("/admin/importacoes/estoque")
                        .file(arquivo())
                        .with(user("administrador").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrosLidos").value(86_919))
                .andExpect(jsonPath("$.produtosAtualizados").value(2_000))
                .andExpect(jsonPath("$.codigosIgnorados").value(84_919));
    }

    private MockMultipartFile arquivo() {
        return new MockMultipartFile(
                "arquivo",
                "inventario.ods",
                "application/vnd.oasis.opendocument.spreadsheet",
                new byte[]{0x50, 0x4b, 0x03, 0x04}
        );
    }
}
