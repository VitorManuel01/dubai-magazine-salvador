package com.ecommerceproject.dubaimagazinesalvador.services.importacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerceproject.dubaimagazinesalvador.domain.importacao.CategoriaImportacaoDTO;
import com.ecommerceproject.dubaimagazinesalvador.domain.importacao.ImportacaoProdutosResponseDTO;
import com.ecommerceproject.dubaimagazinesalvador.domain.importacao.RelacaoProdutosOdsDTO;
import com.ecommerceproject.dubaimagazinesalvador.domain.produto.ProdutoImportacaoDTO;

@JdbcTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ImportacaoProdutosService.class)
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ImportacaoProdutosServiceTest {

    @Autowired
    private ImportacaoProdutosService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    @MockBean
    private LeitorRelacaoProdutosOds leitorRelacaoProdutosOds;

    @BeforeAll
    void aplicarMigrationAnaliticaNoBancoDeTestes() throws Exception {
        Integer colunaNovaExiste = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'produtos'
                  AND column_name = 'nome'
                """, Integer.class);
        if (colunaNovaExiste != null && colunaNovaExiste > 0) {
            return;
        }

        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(
                    connection,
                    new ClassPathResource(
                            "db/migration/V12__adaptar-produtos-relacao-analitica.sql"
                    )
            );
        }
    }

    @Test
    void deveInserirAtualizarEPreservarCamposLocais() {
        when(leitorRelacaoProdutosOds.ler(any())).thenReturn(inventario("5.49"));
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo",
                "inventario.ods",
                "application/vnd.oasis.opendocument.spreadsheet",
                new byte[]{0x50, 0x4b, 0x03, 0x04}
        );

        ImportacaoProdutosResponseDTO primeiraImportacao = service.importar(arquivo);
        assertEquals(1, primeiraImportacao.produtosCriados());
        assertEquals(1, primeiraImportacao.categoriasCriadas());
        assertTrue(Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT exibir_no_site FROM categorias WHERE codigo = '001'",
                Boolean.class
        )));

        jdbcTemplate.update(
                """
                UPDATE produtos
                SET imagem_url = ?, exibir_no_site = TRUE, destaque_na_home = TRUE
                WHERE codigo_santri = ?
                """,
                "/imagens/giz.webp",
                "2672"
        );

        when(leitorRelacaoProdutosOds.ler(any())).thenReturn(inventario("6.99"));
        ImportacaoProdutosResponseDTO segundaImportacao = service.importar(arquivo);

        assertEquals(1, segundaImportacao.produtosAtualizados());
        assertEquals(1, segundaImportacao.categoriasAtualizadas());
        assertEquals(new BigDecimal("6.99"), jdbcTemplate.queryForObject(
                "SELECT preco_sem_ipi FROM produtos WHERE codigo_santri = '2672'",
                BigDecimal.class
        ));
        assertEquals("/imagens/giz.webp", jdbcTemplate.queryForObject(
                "SELECT imagem_url FROM produtos WHERE codigo_santri = '2672'",
                String.class
        ));
        assertTrue(Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT exibir_no_site FROM produtos WHERE codigo_santri = '2672'",
                Boolean.class
        )));
        assertTrue(Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT destaque_na_home FROM produtos WHERE codigo_santri = '2672'",
                Boolean.class
        )));
    }

    @Test
    void deveAcompanharDescricaoAteONomeDoSiteSerPersonalizado() {
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo",
                "inventario.ods",
                "application/vnd.oasis.opendocument.spreadsheet",
                new byte[]{0x50, 0x4b, 0x03, 0x04}
        );

        when(leitorRelacaoProdutosOds.ler(any()))
                .thenReturn(inventario("5.49", "GIZ DE GESSO"));
        service.importar(arquivo);
        assertEquals("GIZ DE GESSO", nomeExibidoDoProduto("2672"));

        when(leitorRelacaoProdutosOds.ler(any()))
                .thenReturn(inventario("5.49", "GIZ ESCOLAR BRANCO"));
        service.importar(arquivo);
        assertEquals("GIZ ESCOLAR BRANCO", nomeExibidoDoProduto("2672"));

        jdbcTemplate.update("""
                UPDATE produtos
                SET nome_exibido_site = 'Giz branco para escola'
                WHERE codigo_santri = '2672'
                """);
        when(leitorRelacaoProdutosOds.ler(any()))
                .thenReturn(inventario("5.49", "GIZ ESCOLAR BRANCO NOVO"));
        service.importar(arquivo);

        assertEquals("GIZ ESCOLAR BRANCO NOVO", jdbcTemplate.queryForObject(
                "SELECT nome FROM produtos WHERE codigo_santri = '2672'",
                String.class
        ));
        assertEquals("Giz branco para escola", nomeExibidoDoProduto("2672"));
    }

    @Test
    void deveManterProdutoAusenteMasTornaLoIndisponivelEOculto() {
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo",
                "relacao-produtos.ods",
                "application/vnd.oasis.opendocument.spreadsheet",
                new byte[]{0x50, 0x4b, 0x03, 0x04}
        );

        when(leitorRelacaoProdutosOds.ler(any()))
                .thenReturn(relacaoComProduto("2672"));
        service.importar(arquivo);
        jdbcTemplate.update("""
                UPDATE produtos
                SET exibir_no_site = TRUE, destaque_na_home = TRUE
                WHERE codigo_santri = '2672'
                """);

        when(leitorRelacaoProdutosOds.ler(any()))
                .thenReturn(relacaoComProduto("2673"));
        service.importar(arquivo);

        assertEquals(1, contar(
                "SELECT COUNT(*) FROM produtos WHERE codigo_santri = '2672'"
        ));
        assertFalse(Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
                SELECT disponivel_ultima_importacao
                FROM produtos
                WHERE codigo_santri = '2672'
                """, Boolean.class)));
        assertFalse(Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
                SELECT exibir_no_site
                FROM produtos
                WHERE codigo_santri = '2672'
                """, Boolean.class)));
        assertFalse(Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
                SELECT destaque_na_home
                FROM produtos
                WHERE codigo_santri = '2672'
                """, Boolean.class)));
    }

    @Test
    void deveExcluirAtivoImobilizadoEOcultarAjustesAteRevisao() {
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo",
                "inventario.ods",
                "application/vnd.oasis.opendocument.spreadsheet",
                new byte[]{0x50, 0x4b, 0x03, 0x04}
        );

        when(leitorRelacaoProdutosOds.ler(any())).thenReturn(inventarioInicialDasRegras());
        service.importar(arquivo);
        jdbcTemplate.update("""
                UPDATE produtos
                SET exibir_no_site = TRUE, destaque_na_home = TRUE
                WHERE codigo_santri IN ('990089', '990123')
                """);

        when(leitorRelacaoProdutosOds.ler(any())).thenReturn(inventarioComCategoriasInternas());
        ImportacaoProdutosResponseDTO importacao = service.importar(arquivo);

        assertEquals(5, importacao.categoriasLidas());
        assertEquals(2, importacao.produtosLidos());
        assertEquals(0, contar(
                "SELECT COUNT(*) FROM produtos WHERE codigo_santri = '990089'"
        ));
        assertEquals(0, contar(
                "SELECT COUNT(*) FROM categorias WHERE codigo = '089' OR codigo LIKE '089.%'"
        ));
        assertFalse(Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT exibir_no_site FROM produtos WHERE codigo_santri = '990123'",
                Boolean.class
        )));
        assertFalse(Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT destaque_na_home FROM produtos WHERE codigo_santri = '990123'",
                Boolean.class
        )));

        jdbcTemplate.update("""
                UPDATE categorias
                SET exibir_no_site = TRUE
                WHERE codigo = '123' OR codigo LIKE '123.%'
                """);
        jdbcTemplate.update("""
                UPDATE produtos
                SET exibir_no_site = TRUE, destaque_na_home = TRUE
                WHERE codigo_santri = '990123'
                """);

        service.importar(arquivo);

        assertFalse(Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT exibir_no_site FROM categorias WHERE codigo = '123'",
                Boolean.class
        )));
        assertTrue(Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT exibir_no_site FROM produtos WHERE codigo_santri = '990123'",
                Boolean.class
        )));
        assertTrue(Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT destaque_na_home FROM produtos WHERE codigo_santri = '990123'",
                Boolean.class
        )));
    }

    private RelacaoProdutosOdsDTO inventario(String precoVenda) {
        return inventario(precoVenda, "GIZ DE GESSO");
    }

    private RelacaoProdutosOdsDTO inventario(String precoVenda, String descricao) {
        CategoriaImportacaoDTO categoria = new CategoriaImportacaoDTO(
                "001",
                "PAPELARIA",
                1,
                "PAPELARIA",
                null
        );
        ProdutoImportacaoDTO produto = produto(
                "2672",
                descricao,
                "001",
                new BigDecimal(precoVenda)
        );
        return new RelacaoProdutosOdsDTO(List.of(categoria), List.of(produto), 0);
    }

    private RelacaoProdutosOdsDTO inventarioInicialDasRegras() {
        CategoriaImportacaoDTO categoria = categoria("777", "CATEGORIA DE TESTE", null);
        return new RelacaoProdutosOdsDTO(
                List.of(categoria),
                List.of(
                        produto("990089", "VEICULO DE TESTE", "777"),
                        produto("990123", "PRODUTO EM AJUSTE", "777"),
                        produto("990777", "PRODUTO COMERCIAL", "777")
                ),
                0
        );
    }

    private RelacaoProdutosOdsDTO relacaoComProduto(String codigoProduto) {
        CategoriaImportacaoDTO categoria = categoria(
                "777",
                "CATEGORIA DE TESTE",
                null
        );
        return new RelacaoProdutosOdsDTO(
                List.of(categoria),
                List.of(produto(codigoProduto, "PRODUTO " + codigoProduto, "777")),
                0
        );
    }

    private RelacaoProdutosOdsDTO inventarioComCategoriasInternas() {
        return new RelacaoProdutosOdsDTO(
                List.of(
                        categoria("089", "ATIVO IMOBILIZADO", null),
                        categoria("089.001", "ATIVO IMOBILIZADO", "089"),
                        categoria("089.001.0001", "ATIVO IMOBILIZADO", "089.001"),
                        categoria(
                                "089.001.0001.0001",
                                "ATIVO IMOBILIZADO",
                                "089.001.0001"
                        ),
                        categoria("123", "AJUSTES DE GRUPOS", null),
                        categoria("123.001", "AJUSTES DE GRUPOS", "123"),
                        categoria("123.001.0001", "AJUSTES DE GRUPOS", "123.001"),
                        categoria(
                                "123.001.0001.0001",
                                "AJUSTES DE GRUPOS",
                                "123.001.0001"
                        ),
                        categoria("777", "CATEGORIA DE TESTE", null)
                ),
                List.of(
                        produto("990089", "VEICULO DE TESTE", "089.001.0001.0001"),
                        produto("990123", "PRODUTO EM AJUSTE", "123.001.0001.0001"),
                        produto("990777", "PRODUTO COMERCIAL", "777")
                ),
                0
        );
    }

    private CategoriaImportacaoDTO categoria(String codigo, String nome, String codigoPai) {
        int nivel = codigo.split("\\.").length;
        return new CategoriaImportacaoDTO(codigo, nome, nivel, nome, codigoPai);
    }

    private ProdutoImportacaoDTO produto(
            String codigo,
            String descricao,
            String categoriaCodigo
    ) {
        return produto(codigo, descricao, categoriaCodigo, BigDecimal.TEN);
    }

    private ProdutoImportacaoDTO produto(
            String codigo,
            String descricao,
            String categoriaCodigo,
            BigDecimal precoSemIpi
    ) {
        return new ProdutoImportacaoDTO(
                codigo,
                descricao,
                "00000000",
                descricao,
                "1 - FABRICANTE TESTE",
                "TESTE",
                true,
                "UN",
                "CX",
                LocalDate.of(2026, 7, 30),
                codigo,
                "7890000000000",
                false,
                BigDecimal.ONE,
                precoSemIpi,
                new BigDecimal("10.00"),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "0 - Nacional",
                null,
                null,
                BigDecimal.ZERO,
                null,
                categoriaCodigo
        );
    }

    private int contar(String sql) {
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }

    private String nomeExibidoDoProduto(String codigoSantri) {
        return jdbcTemplate.queryForObject(
                "SELECT nome_exibido_site FROM produtos WHERE codigo_santri = ?",
                String.class,
                codigoSantri
        );
    }
}
