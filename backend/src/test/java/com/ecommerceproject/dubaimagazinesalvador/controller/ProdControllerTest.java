package com.ecommerceproject.dubaimagazinesalvador.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ecommerceproject.dubaimagazinesalvador.config.MockMvcConfig;
import com.ecommerceproject.dubaimagazinesalvador.config.TestSecurityConfig;
import com.ecommerceproject.dubaimagazinesalvador.domain.categoria.Categoria;
import com.ecommerceproject.dubaimagazinesalvador.domain.produto.Produto;
import com.ecommerceproject.dubaimagazinesalvador.domain.produto.ProdutoRequestDTO;
import com.ecommerceproject.dubaimagazinesalvador.domain.produto.ProdutoResponseDTO;
import com.ecommerceproject.dubaimagazinesalvador.infra.security.TokenService;
import com.ecommerceproject.dubaimagazinesalvador.repositories.CategoriaRepository;
import com.ecommerceproject.dubaimagazinesalvador.repositories.ProdutoRepository;
import com.ecommerceproject.dubaimagazinesalvador.repositories.UsuarioRepository;
import com.ecommerceproject.dubaimagazinesalvador.services.produto.ApresentacaoProdutoService;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManager;

@Import(MockMvcConfig.class)
@WebMvcTest(ProdController.class)
@AutoConfigureMockMvc
@ContextConfiguration(classes = {TestSecurityConfig.class, ProdController.class})
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProdControllerTest {

    private static final String CATEGORIA_CODIGO = "001.003.0006.0002";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EntityManager entityManager;

    @MockBean
    private TokenService tokenService;

    @MockBean
    private UsuarioRepository usuarioRepository;

    @MockBean
    private ProdutoRepository produtoRepository;

    @MockBean
    private CategoriaRepository categoriaRepository;

    @MockBean
    private ApresentacaoProdutoService apresentacaoProdutoService;

    private ObjectMapper objectMapper;
    private Categoria categoria;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        categoria = new Categoria(
                CATEGORIA_CODIGO,
                "GIZ DE CERA",
                4,
                "PAPELARIA > ESCOLAR > COLORIR > GIZ DE CERA",
                null,
                true
        );
        when(categoriaRepository.findById(CATEGORIA_CODIGO)).thenReturn(Optional.of(categoria));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve salvar um produto usando o código do Santri")
    void saveProduto() throws Exception {
        ProdutoRequestDTO requestDTO = criarRequest("2.672", "Produto Exemplo");
        Produto produto = new Produto(requestDTO, categoria);

        when(produtoRepository.save(any(Produto.class))).thenReturn(produto);

        mockMvc.perform(post("/produto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigoSantri").value("2.672"))
                .andExpect(jsonPath("$.nomeExibidoSite").value("Produto Exemplo"))
                .andExpect(jsonPath("$.categoriaCaminho")
                        .value("PAPELARIA > ESCOLAR > COLORIR > GIZ DE CERA"));

        verify(produtoRepository, times(1)).save(any(Produto.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve adicionar todos os produtos do banco a uma lista")
    void getAll() throws Exception {
        List<Produto> produtos = List.of(
                criarProduto("2.672", "Produto 1"),
                criarProduto("2.673", "Produto 2")
        );

        when(produtoRepository.findCatalogoPorCategoria(
                isNull(),
                isNull(),
                eq(true),
                eq(false),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(produtos));

        mockMvc.perform(get("/produto")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].descricao").value("Produto 1"))
                .andExpect(jsonPath("$.content[1].descricao").value("Produto 2"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve filtrar produtos pela categoria e suas descendentes")
    void getAllByCategoria() throws Exception {
        List<Produto> produtos = List.of(criarProduto("2.672", "Produto 1"));
        when(produtoRepository.findCatalogoPorCategoria(
                eq("001"),
                isNull(),
                eq(true),
                eq(false),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(produtos));

        mockMvc.perform(get("/produto")
                        .param("categoriaCodigo", "001")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].descricao").value("Produto 1"));

        verify(produtoRepository).findCatalogoPorCategoria(
                eq("001"),
                isNull(),
                eq(true),
                eq(false),
                any(Pageable.class)
        );
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve pesquisar produtos pelo termo informado")
    void getAllByBusca() throws Exception {
        List<Produto> produtos = List.of(criarProduto("2.672", "Caderno escolar"));
        when(produtoRepository.findCatalogoPorCategoria(
                isNull(),
                eq("caderno"),
                eq(true),
                eq(false),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(produtos));

        mockMvc.perform(get("/produto")
                        .param("busca", "  caderno  "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].descricao").value("Caderno escolar"));

        verify(produtoRepository).findCatalogoPorCategoria(
                isNull(),
                eq("caderno"),
                eq(true),
                eq(false),
                any(Pageable.class)
        );
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    @DisplayName("Deve omitir produtos não publicados para usuários comuns")
    void getAllPublico() throws Exception {
        when(produtoRepository.findCatalogoPorCategoria(
                isNull(),
                isNull(),
                eq(false),
                eq(false),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/produto"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());

        verify(produtoRepository).findCatalogoPorCategoria(
                isNull(),
                isNull(),
                eq(false),
                eq(false),
                any(Pageable.class)
        );
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    @DisplayName("Deve retornar somente os produtos selecionados para a loja")
    void getAllDestaquesDaHome() throws Exception {
        Produto produto = criarProduto("2.672", "Produto selecionado");
        when(produtoRepository.findCatalogoPorCategoria(
                isNull(),
                isNull(),
                eq(false),
                eq(true),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(produto)));

        mockMvc.perform(get("/produto").param("somenteDestaques", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].descricao").value("Produto selecionado"));

        verify(produtoRepository).findCatalogoPorCategoria(
                isNull(),
                isNull(),
                eq(false),
                eq(true),
                any(Pageable.class)
        );
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve deletar um produto")
    void deleteProduto() throws Exception {
        String codigoSantri = "2.672";
        Produto produto = criarProduto(codigoSantri, "Produto 1");
        when(produtoRepository.findById(codigoSantri)).thenReturn(Optional.of(produto));

        mockMvc.perform(delete("/produto/" + codigoSantri)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("Produto deletado com sucesso!"));

        verify(produtoRepository, times(1)).delete(produto);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve alterar somente a apresentação do produto")
    void updateApresentacaoProduto() throws Exception {
        String codigoSantri = "2.672";
        Produto produto = criarProduto(codigoSantri, "Produto 1");
        produto.atualizarApresentacao("Nome público", true, true, null);
        when(apresentacaoProdutoService.atualizar(
                codigoSantri,
                "Nome público",
                true,
                true,
                null
        ))
                .thenReturn(new ProdutoResponseDTO(produto));

        mockMvc.perform(multipart("/produto/" + codigoSantri + "/apresentacao")
                        .param("nomeExibidoSite", "Nome público")
                        .param("exibirNoSite", "true")
                        .param("destaqueNaHome", "true")
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoSantri").value(codigoSantri))
                .andExpect(jsonPath("$.nomeExibidoSite").value("Nome público"));

        verify(apresentacaoProdutoService).atualizar(
                codigoSantri,
                "Nome público",
                true,
                true,
                null
        );
    }

    private ProdutoRequestDTO criarRequest(String codigoSantri, String descricao) {
        return new ProdutoRequestDTO(
                codigoSantri,
                descricao,
                descricao,
                "96099000",
                "UN",
                "ACRILEX",
                "090120000",
                new BigDecimal("100.000"),
                new BigDecimal("9.99"),
                BigDecimal.ZERO,
                CATEGORIA_CODIGO,
                "https://exemplo.com/imagem.jpg",
                true
        );
    }

    private Produto criarProduto(String codigoSantri, String descricao) {
        return new Produto(criarRequest(codigoSantri, descricao), categoria);
    }
}
