package com.ecommerceproject.dubaimagazinesalvador.services.vitrine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.doReturn;

import java.math.BigDecimal;
import java.util.List;
import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

import com.ecommerceproject.dubaimagazinesalvador.domain.categoria.Categoria;
import com.ecommerceproject.dubaimagazinesalvador.domain.produto.Produto;
import com.ecommerceproject.dubaimagazinesalvador.domain.produto.ProdutoRequestDTO;
import com.ecommerceproject.dubaimagazinesalvador.domain.vitrine.loja.ProdutoVitrineLoja;
import com.ecommerceproject.dubaimagazinesalvador.domain.vitrine.loja.ProdutoVitrineLojaRequestDTO;
import com.ecommerceproject.dubaimagazinesalvador.domain.vitrine.loja.SecaoVitrineLoja;
import com.ecommerceproject.dubaimagazinesalvador.domain.vitrine.loja.SecaoVitrineLojaRequestDTO;
import com.ecommerceproject.dubaimagazinesalvador.domain.vitrine.loja.VitrineLoja;
import com.ecommerceproject.dubaimagazinesalvador.domain.vitrine.loja.VitrineLojaRequestDTO;
import com.ecommerceproject.dubaimagazinesalvador.repositories.ProdutoRepository;
import com.ecommerceproject.dubaimagazinesalvador.repositories.VitrineLojaRepository;

@ExtendWith(MockitoExtension.class)
class VitrineLojaServiceTest {

    @Mock
    private VitrineLojaRepository vitrineRepository;

    @Mock
    private ProdutoRepository produtoRepository;

    private VitrineLojaService service;
    private Produto scooterAzul;
    private Produto scooterPreta;

    @BeforeEach
    void setUp() {
        service = new VitrineLojaService(vitrineRepository, produtoRepository);
        Categoria categoria = new Categoria(
                "123",
                "AJUSTES DE GRUPOS",
                1,
                "AJUSTES DE GRUPOS",
                null,
                false
        );
        scooterAzul = produto("855437", "Scooter elétrica azul", categoria);
        scooterPreta = produto("855438", "Scooter elétrica preta", categoria);
    }

    @Test
    void deveCriarVitrineComProdutosImagensESecoes() {
        when(produtoRepository.findById("855437")).thenReturn(Optional.of(scooterAzul));
        when(produtoRepository.findById("855438")).thenReturn(Optional.of(scooterPreta));
        when(vitrineRepository.encontrarProdutosEmOutrasVitrines(any(), eq(null)))
                .thenReturn(List.of());
        when(vitrineRepository.saveAndFlush(any(VitrineLoja.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var resposta = service.criar(requestValido());

        assertTrue(resposta.ativo());
        assertEquals(2, resposta.opcoes().size());
        assertEquals("Azul", resposta.opcoes().getFirst().rotuloOpcao());
        assertEquals("Scooter elétrica azul",
                resposta.opcoes().getFirst().produto().nomeExibidoSite());
        assertEquals(2, resposta.opcoes().getFirst().imagens().size());
        assertEquals("Motor", resposta.opcoes().getFirst().secoes().getFirst().titulo());
        assertEquals("Motor elétrico de 500 W.",
                resposta.opcoes().getFirst().secoes().getFirst().conteudo());
        verify(vitrineRepository).saveAndFlush(any(VitrineLoja.class));
    }

    @Test
    void devePesquisarSomenteVitrinesAtivasParaConsultaInterna() {
        VitrineLoja vitrine = new VitrineLoja(true);
        ProdutoVitrineLoja opcao = new ProdutoVitrineLoja(
                scooterAzul,
                "Azul",
                0,
                List.of("/catalogo/imagens/11111111-1111-1111-1111-111111111111.webp")
        );
        opcao.adicionarSecao(new SecaoVitrineLoja("Motor", "500 W", 0));
        vitrine.adicionarOpcao(opcao);
        when(vitrineRepository.pesquisar(eq(true), eq("scooter"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(vitrine)));

        var resposta = service.pesquisar(" scooter ", 0, 12, true);

        assertEquals(1, resposta.getTotalElements());
        assertEquals("855437",
                resposta.getContent().getFirst().opcoes().getFirst().produto().codigoSantri());
        verify(vitrineRepository).pesquisar(eq(true), eq("scooter"), any(Pageable.class));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void deveListarVitrineComFotosLegadasInvalidasSemErro(boolean somenteAtivas) {
        Produto produtoLegado = spy(scooterAzul);
        List<String> urls = Arrays.asList(
                "/uploads/produtos/11111111-1111-1111-1111-111111111111.webp",
                "https://externo.example/foto.jpg", null, "", "/uploads/produtos/antiga.jpg",
                "/catalogo/imagens/22222222-2222-2222-2222-222222222222.png");
        doReturn(urls).when(produtoLegado).getUrlsImagens();
        VitrineLoja vitrine = new VitrineLoja(true);
        vitrine.adicionarOpcao(new ProdutoVitrineLoja(produtoLegado, "Azul", 0, List.of()));
        when(vitrineRepository.pesquisar(eq(somenteAtivas), eq("scooter"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(vitrine)));

        var resposta = service.pesquisar("scooter", 0, 12, somenteAtivas);

        assertEquals(List.of(
                "/catalogo/imagens/11111111-1111-1111-1111-111111111111.webp",
                "/catalogo/imagens/22222222-2222-2222-2222-222222222222.png"),
                resposta.getContent().getFirst().opcoes().getFirst().imagens());
        assertEquals(6, urls.size()); // A leitura não remove fotos do cadastro.
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void deveListarVitrineMesmoQuandoTodasAsFotosSaoInvalidas(boolean somenteAtivas) {
        Produto produtoLegado = spy(scooterAzul);
        doReturn(Arrays.asList(null, "", "/uploads/produtos/antiga.jpg"))
                .when(produtoLegado).getUrlsImagens();
        VitrineLoja vitrine = new VitrineLoja(true);
        vitrine.adicionarOpcao(new ProdutoVitrineLoja(produtoLegado, "Azul", 0, List.of()));
        when(vitrineRepository.pesquisar(eq(somenteAtivas), eq("scooter"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(vitrine)));

        var resposta = service.pesquisar("scooter", 0, 12, somenteAtivas);

        assertTrue(resposta.getContent().getFirst().opcoes().getFirst().imagens().isEmpty());
        assertEquals("855437", resposta.getContent().getFirst().opcoes().getFirst().produto().codigoSantri());
    }

    @Test
    void devePesquisarTodosOsProdutosParaAdministracaoMesmoOcultos() {
        when(produtoRepository.findCatalogoPorCategoria(
                eq(null),
                eq("scooter"),
                eq(true),
                eq(false),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(scooterAzul, scooterPreta)));

        var resposta = service.pesquisarProdutosParaAdministracao(
                " scooter ",
                0,
                60
        );

        assertEquals(2, resposta.getTotalElements());
        assertEquals("855437", resposta.getContent().getFirst().codigoSantri());
        assertFalse(resposta.getContent().getFirst().exibirNoSite());
        verify(produtoRepository).findCatalogoPorCategoria(
                null,
                "scooter",
                true,
                false,
                PageRequest.of(0, 60)
        );
    }

    @Test
    void devePermitirRascunhoSemSecoesMasNaoVitrineAtiva() {
        when(produtoRepository.findById("855437")).thenReturn(Optional.of(scooterAzul));
        when(vitrineRepository.encontrarProdutosEmOutrasVitrines(any(), eq(null)))
                .thenReturn(List.of());
        when(vitrineRepository.saveAndFlush(any(VitrineLoja.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        var opcaoSemSecao = new ProdutoVitrineLojaRequestDTO(
                "855437",
                "Azul",
                0,
                List.of(),
                List.of()
        );

        var rascunho = service.criar(new VitrineLojaRequestDTO(false, List.of(opcaoSemSecao)));
        assertFalse(rascunho.ativo());

        assertThrows(
                ResponseStatusException.class,
                () -> service.criar(new VitrineLojaRequestDTO(true, List.of(opcaoSemSecao)))
        );
    }

    @Test
    void deveImpedirProdutoEmDuasVitrines() {
        when(produtoRepository.findById("855437")).thenReturn(Optional.of(scooterAzul));
        when(produtoRepository.findById("855438")).thenReturn(Optional.of(scooterPreta));
        when(vitrineRepository.encontrarProdutosEmOutrasVitrines(any(), eq(null)))
                .thenReturn(List.of("855437"));

        ResponseStatusException erro = assertThrows(
                ResponseStatusException.class,
                () -> service.criar(requestValido())
        );

        assertEquals(409, erro.getStatusCode().value());
    }

    private VitrineLojaRequestDTO requestValido() {
        return new VitrineLojaRequestDTO(
                true,
                List.of(
                        opcao("855437", "Azul", "/catalogo/imagens/11111111-1111-1111-1111-111111111111.webp"),
                        opcao("855438", "Preta", "/catalogo/imagens/22222222-2222-2222-2222-222222222222.webp")
                )
        );
    }

    private ProdutoVitrineLojaRequestDTO opcao(
            String codigo,
            String rotulo,
            String imagem
    ) {
        return new ProdutoVitrineLojaRequestDTO(
                codigo,
                rotulo,
                0,
                List.of(
                        imagem,
                        "/catalogo/imagens/33333333-3333-4333-8333-333333333333.webp"
                ),
                List.of(new SecaoVitrineLojaRequestDTO(
                        "Motor",
                        "Motor elétrico de 500 W.",
                        0
                ))
        );
    }

    private Produto produto(String codigo, String nome, Categoria categoria) {
        return new Produto(
                new ProdutoRequestDTO(
                        codigo,
                        "DESCRICAO INTERNA DO SANTRI",
                        nome,
                        "87116000",
                        "UN",
                        "DUBAI",
                        codigo,
                        BigDecimal.TEN,
                        new BigDecimal("3703.62"),
                        BigDecimal.ZERO,
                        categoria.getCodigo(),
                        null,
                        false
                ),
                categoria
        );
    }
}
