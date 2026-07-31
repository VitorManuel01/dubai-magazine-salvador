package com.ecommerceproject.dubaimagazinesalvador.services.vitrine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
                List.of("/imagens/scooter-azul.webp")
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
                        opcao("855437", "Azul", "/imagens/scooter-azul.webp"),
                        opcao("855438", "Preta", "/imagens/scooter-preta.webp")
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
                List.of(imagem, imagem + "?detalhe=1"),
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
