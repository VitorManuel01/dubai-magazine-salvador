package com.ecommerceproject.dubaimagazinesalvador.services.importacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.stream.XMLInputFactory;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.ecommerceproject.dubaimagazinesalvador.domain.importacao.RelacaoProdutosOdsDTO;
import com.ecommerceproject.dubaimagazinesalvador.domain.importacao.RelacaoPromocoesOdsDTO;
import com.ecommerceproject.dubaimagazinesalvador.domain.importacao.EstoqueProdutoImportacaoDTO;
import com.ecommerceproject.dubaimagazinesalvador.domain.produto.ProdutoImportacaoDTO;

class LeitorRelacaoProdutosOdsTest {

    private static final List<String> CABECALHOS = List.of(
            "Código", "Nome", "NCM", "Nome de compra", "Fabricante", "Marca",
            "Ativo?", "Und. venda", "Und. compra", "Tipo controle de estoque",
            "Aceita estoque negativo?", "Data cadastro", "Código original",
            "Código de barras", "Bloqueado para compras",
            "Data inicial comissão especial", "Data final comissão especial",
            "Valor comissao especial", "Percentual comissão especial", "Estoque",
            "Preço", "Múltiplo de venda", "Múltiplo de compra unitário",
            "Múltiplo de compra secundário", "Und. entrega", "Pronta entrega",
            "Kit", "Quantidade dias garantia do fabricante",
            "Quantidade dias vencimento gerar bloqueio",
            "Valor frete adicional na venda", "% frete adicional na venda",
            "Valor frete fixo (manifesto)", "Usuário de inserção", "% IPI entrada",
            "Peso da unidade", "Altura da unidade", "Largura da unidade",
            "Comprimento da unidade", "Volume da unidade (m³)", "Volume em litros",
            "Peso da caixa", "Altura da caixa", "Largura da caixa",
            "Comprimento da caixa", "Lastro do palete", "Camada do palete",
            "Capacidade de empilhamento do palete", "Armazena endereço duplo",
            "Tipo de estrutura de armazenagem", "Possui dados de armazenagem",
            "Possui endereços de picking", "Unidade padrão contagem estoque",
            "Origem", "Industrializado", "Insumo", "% Máx. aprov. IPI", "Número FCI"
    );

    private final LeitorRelacaoProdutosOds leitor = new LeitorRelacaoProdutosOds();

    @Test
    void deveLerRelacaoAnaliticaManterEsgotadosEFiltrarInativos() throws Exception {
        RelacaoProdutosOdsDTO relacao = leitor.ler(
                new ByteArrayInputStream(odsExemplo())
        );

        assertEquals(4, relacao.categorias().size());
        assertEquals(
                "PAPELARIA > ESCOLAR > COLORIR > GIZ DE CERA",
                relacao.categorias().get(3).caminho()
        );
        assertEquals(2, relacao.produtos().size());
        assertEquals(1, relacao.linhasIgnoradas());

        ProdutoImportacaoDTO produto = relacao.produtos().getFirst();
        assertEquals("2672", produto.codigoSantri());
        assertEquals("001.003.0006.0002", produto.categoriaCodigo());
        assertEquals("GIZ DE GESSO COMUM", produto.nome());
        assertEquals("GIZ DE GESSO C/64 UN", produto.nomeCompra());
        assertEquals(new BigDecimal("525.000"), produto.estoque());
        assertEquals(new BigDecimal("5.49"), produto.precoSemIpi());
        assertEquals(new BigDecimal("13.00"), produto.percentualIpiEntrada());
        assertEquals(new BigDecimal("0.015833"), produto.pesoUnidade());
        assertEquals(LocalDate.of(2019, 8, 1), produto.dataCadastro());
        assertEquals("7897464700019", produto.codigoBarras());
        assertTrue(produto.industrializado());
        assertFalse(produto.insumo());
        assertEquals(new BigDecimal("0.000"), relacao.produtos().get(1).estoque());
    }

    @Test
    void deveLerSomenteCodigoEQuantidadeDoInventario() throws Exception {
        List<EstoqueProdutoImportacaoDTO> registros = new ArrayList<>();

        int quantidade = leitor.lerInventario(
                new ByteArrayInputStream(odsInventarioExemplo()),
                registros::add
        );

        assertEquals(3, quantidade);
        assertEquals("2672", registros.get(0).codigoSantri());
        assertEquals(new BigDecimal("491.000"), registros.get(0).quantidade());
        assertEquals("819971", registros.get(1).codigoSantri());
        assertEquals(new BigDecimal("0.000"), registros.get(1).quantidade());
        assertEquals("800068", registros.get(2).codigoSantri());
    }

    @Test
    void deveRejeitarCelulasComFormula() throws Exception {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <office:document-content
                    xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
                    xmlns:table="urn:oasis:names:tc:opendocument:xmlns:table:1.0"
                    xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0">
                  <office:body><office:spreadsheet><table:table table:name="Teste">
                    <table:table-row>
                      <table:table-cell table:formula="of:=1+1"><text:p>2</text:p></table:table-cell>
                    </table:table-row>
                  </table:table></office:spreadsheet></office:body>
                </office:document-content>
                """;

        assertThrows(
                ImportacaoOdsException.class,
                () -> leitor.ler(new ByteArrayInputStream(empacotarOds(
                        xml.getBytes(StandardCharsets.UTF_8)
                )))
        );
    }

    @Test
    void deveRejeitarCelulaComMaisDe75Caracteres() throws Exception {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <office:document-content
                    xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
                    xmlns:table="urn:oasis:names:tc:opendocument:xmlns:table:1.0"
                    xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0">
                  <office:body><office:spreadsheet><table:table table:name="Teste">
                    <table:table-row>
                      <table:table-cell><text:p>%s</text:p></table:table-cell>
                    </table:table-row>
                  </table:table></office:spreadsheet></office:body>
                </office:document-content>
                """.formatted("A".repeat(76));

        ImportacaoOdsException exception = assertThrows(
                ImportacaoOdsException.class,
                () -> leitor.ler(new ByteArrayInputStream(empacotarOds(
                        xml.getBytes(StandardCharsets.UTF_8)
                )))
        );

        assertTrue(exception.getMessage().contains("mais de 75 caracteres"));
    }

    @Test
    void deveRejeitarPlanilhaComMaisDe25MilLinhas() throws Exception {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <office:document-content
                    xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
                    xmlns:table="urn:oasis:names:tc:opendocument:xmlns:table:1.0">
                  <office:body><office:spreadsheet><table:table table:name="Teste">
                    %s
                  </table:table></office:spreadsheet></office:body>
                </office:document-content>
                """.formatted("<table:table-row/>".repeat(25_001));

        ImportacaoOdsException exception = assertThrows(
                ImportacaoOdsException.class,
                () -> leitor.ler(new ByteArrayInputStream(empacotarOds(
                        xml.getBytes(StandardCharsets.UTF_8)
                )))
        );

        assertTrue(exception.getMessage().contains("25.000 linhas"));
    }

    @Test
    void deveRejeitarDtdEEntidadeXml() throws Exception {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE documento [
                  <!ENTITY arquivoExterno SYSTEM "file:///arquivo-que-nao-deve-ser-lido">
                ]>
                <office:document-content
                    xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
                    xmlns:table="urn:oasis:names:tc:opendocument:xmlns:table:1.0"
                    xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0">
                  <office:body><office:spreadsheet><table:table table:name="Teste">
                    <table:table-row>
                      <table:table-cell><text:p>&arquivoExterno;</text:p></table:table-cell>
                    </table:table-row>
                  </table:table></office:spreadsheet></office:body>
                </office:document-content>
                """;

        assertThrows(
                ImportacaoOdsException.class,
                () -> leitor.ler(new ByteArrayInputStream(empacotarOds(
                        xml.getBytes(StandardCharsets.UTF_8)
                )))
        );
    }

    @Test
    void deveFalharFechadoQuandoParserNaoAceitarProtecaoObrigatoria() {
        XMLInputFactory factory = mock(XMLInputFactory.class);
        doThrow(new IllegalArgumentException("Propriedade não suportada"))
                .when(factory)
                .setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);

        ImportacaoOdsException exception = assertThrows(
                ImportacaoOdsException.class,
                () -> leitor.configurarXmlSeguro(factory)
        );

        assertTrue(exception.getMessage().contains("proteções obrigatórias"));
    }

    @Test
    void deveFalharFechadoQuandoParserIgnorarLimitesDeRecursos() {
        XMLInputFactory factory = mock(XMLInputFactory.class);
        when(factory.getProperty(XMLInputFactory.SUPPORT_DTD)).thenReturn(Boolean.FALSE);
        when(factory.getProperty("javax.xml.stream.isSupportingExternalEntities")).thenReturn(Boolean.FALSE);
        when(factory.getProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES)).thenReturn(Boolean.FALSE);
        when(factory.getProperty(XMLInputFactory.IS_COALESCING)).thenReturn(Boolean.FALSE);
        when(factory.getProperty("jdk.xml.maxElementDepth")).thenReturn("0");

        ImportacaoOdsException erro = assertThrows(ImportacaoOdsException.class,
                () -> leitor.configurarXmlSeguro(factory));

        assertTrue(erro.getMessage().contains("limites obrigatórios"));
    }

    @Test
    void deveRejeitarProfundidadeXmlExcessivaMesmoForaDeCelulas() throws Exception {
        String xml = "<root>" + "<nivel>".repeat(65) + "</nivel>".repeat(65) + "</root>";

        assertThrows(ImportacaoOdsException.class,
                () -> leitor.ler(new ByteArrayInputStream(empacotarOds(xml.getBytes(StandardCharsets.UTF_8)))));
    }

    @Test
    void deveRejeitarAtributoXmlExcessivoAntesDeProcessarProdutos() throws Exception {
        String xml = "<root atributo=\"" + "A".repeat(16_385) + "\"/>";

        ImportacaoOdsException erro = assertThrows(ImportacaoOdsException.class,
                () -> leitor.ler(new ByteArrayInputStream(empacotarOds(xml.getBytes(StandardCharsets.UTF_8)))));

        assertTrue(erro.getMessage().contains("atributo XML"));
    }

    @Test
    void deveRejeitarExpoentesENumerosExcessivosAntesDeEntregarEstoque() throws Exception {
        for (String quantidade : List.of("1E+2147483647", "1E-2147483647", "999999999999999999999999999999999999")) {
            List<EstoqueProdutoImportacaoDTO> registros = new ArrayList<>();
            String xml = """
                    <office:document-content
                        xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
                        xmlns:table="urn:oasis:names:tc:opendocument:xmlns:table:1.0"
                        xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0">
                      <office:body><office:spreadsheet><table:table>
                        %s%s
                      </table:table></office:spreadsheet></office:body>
                    </office:document-content>
                    """.formatted(linha(List.of("Produto", "Quantidade")), linha(List.of("123", quantidade)));

            assertThrows(ImportacaoOdsException.class,
                    () -> leitor.lerInventario(new ByteArrayInputStream(
                            empacotarOds(xml.getBytes(StandardCharsets.UTF_8))), registros::add));
            assertTrue(registros.isEmpty());
        }
    }

    @Test
    void deveRejeitarEntradaComCompactacaoSuspeita() throws Exception {
        byte[] conteudoRepetitivo = new byte[2 * 1024 * 1024];
        Arrays.fill(conteudoRepetitivo, (byte) 'A');

        assertThrows(
                ImportacaoOdsException.class,
                () -> leitor.ler(new ByteArrayInputStream(empacotarOds(conteudoRepetitivo)))
        );
    }

    @Test
    void deveLerPromocoesEIgnorarLinhasDeAgrupamento() throws Exception {
        RelacaoPromocoesOdsDTO relacao = leitor.lerPromocoes(
                new ByteArrayInputStream(odsPromocoesExemplo())
        );

        assertEquals(2, relacao.promocoes().size());
        assertEquals(1, relacao.linhasIgnoradas());
        assertEquals("838563", relacao.promocoes().getFirst().codigoSantri());
        assertEquals(new BigDecimal("89.90"), relacao.promocoes().getFirst().precoPromocao());
        assertTrue(relacao.promocoes().getFirst().emPromocao());
        assertFalse(relacao.promocoes().getLast().emPromocao());
    }

    @Test
    void deveLerRelacaoRealDePromocoesQuandoCaminhoForInformado() throws Exception {
        String caminho = System.getProperty("promocoes.ods.file");
        Assumptions.assumeTrue(caminho != null && Files.exists(Path.of(caminho)));

        RelacaoPromocoesOdsDTO relacao;
        try (FileInputStream input = new FileInputStream(caminho)) {
            relacao = leitor.lerPromocoes(input);
        }

        assertEquals(1_595, relacao.promocoes().size());
        assertEquals(1_579, relacao.promocoes().stream().filter(p -> p.emPromocao()).count());
        assertEquals(
                relacao.promocoes().size(),
                relacao.promocoes().stream().map(p -> p.codigoSantri()).distinct().count()
        );
    }

    @Test
    void deveLerARelacaoRealQuandoCaminhoForInformado() throws Exception {
        String caminho = System.getProperty("ods.file");
        Assumptions.assumeTrue(caminho != null && Files.exists(Path.of(caminho)));

        RelacaoProdutosOdsDTO relacao;
        try (FileInputStream input = new FileInputStream(caminho)) {
            relacao = leitor.ler(input);
        }

        assertEquals(1_855, relacao.categorias().size());
        assertEquals(16_224, relacao.produtos().size());

        Set<String> codigosCategorias = relacao.categorias().stream()
                .map(categoria -> categoria.codigo())
                .collect(Collectors.toSet());

        assertEquals(
                relacao.produtos().size(),
                relacao.produtos().stream()
                        .map(ProdutoImportacaoDTO::codigoSantri)
                        .distinct()
                        .count()
        );
        assertTrue(relacao.produtos().stream()
                .allMatch(produto -> produto.ativoSantri()
                        && produto.estoque() != null));
        assertTrue(relacao.produtos().stream()
                .allMatch(produto -> produto.codigoSantri() != null
                        && !produto.codigoSantri().isBlank()
                        && produto.nome() != null
                        && !produto.nome().isBlank()
                        && codigosCategorias.contains(produto.categoriaCodigo())));
        assertTrue(relacao.categorias().stream()
                .allMatch(categoria -> categoria.nivel() == 1
                        ? categoria.categoriaPaiCodigo() == null
                        : codigosCategorias.contains(categoria.categoriaPaiCodigo())));
        assertTrue(relacao.categorias().stream()
                .noneMatch(categoria -> categoria.codigo().equals("089")
                        || categoria.codigo().startsWith("089.")));
        assertTrue(relacao.produtos().stream()
                .noneMatch(produto -> produto.categoriaCodigo().equals("089")
                        || produto.categoriaCodigo().startsWith("089.")));
    }

    @Test
    void deveLerInventarioRealQuandoCaminhoForInformado() throws Exception {
        String caminho = System.getProperty("inventario.ods.file");
        Assumptions.assumeTrue(caminho != null && Files.exists(Path.of(caminho)));
        int[] esgotados = {0};

        int registros;
        try (FileInputStream input = new FileInputStream(caminho)) {
            registros = leitor.lerInventario(input, item -> {
                if (item.quantidade().signum() == 0) {
                    esgotados[0]++;
                }
            });
        }

        assertEquals(86_919, registros);
        assertEquals(32_869, esgotados[0]);
    }

    private byte[] odsExemplo() throws Exception {
        StringBuilder linhas = new StringBuilder();
        linhas.append(linha(CABECALHOS));
        linhas.append(linha(List.of("001", "PAPELARIA")));
        linhas.append(linha(List.of("001.003", "ESCOLAR")));
        linhas.append(linha(List.of("001.003.0006", "COLORIR")));
        linhas.append(linha(List.of("001.003.0006.0002", "GIZ DE CERA")));
        linhas.append(linha(produto("2.672", "Sim", "525,000")));
        linhas.append(linha(produto("2.673", "Sim", "0,000")));
        linhas.append(linha(produto("2.674", "Não", "10,000")));

        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <office:document-content
                    xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
                    xmlns:table="urn:oasis:names:tc:opendocument:xmlns:table:1.0"
                    xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0">
                  <office:body>
                    <office:spreadsheet>
                      <table:table table:name="Relação analítica">
                        %s
                      </table:table>
                    </office:spreadsheet>
                  </office:body>
                </office:document-content>
                """.formatted(linhas);

        return empacotarOds(xml.getBytes(StandardCharsets.UTF_8));
    }

    private byte[] odsInventarioExemplo() throws Exception {
        StringBuilder linhasPrincipais = new StringBuilder();
        linhasPrincipais.append(linha(List.of("Santri ADM", "", "Emitido em 14/08/2026")));
        linhasPrincipais.append(linha(List.of(
                "Produto", "Descrição", "", "Cód. fiscal", "Und", "Marca",
                "Cód. Original", "Quantidade", "Custo Médio", "Cust.Méd(IVA)", "Total"
        )));
        linhasPrincipais.append(linha(List.of("001 - PAPELARIA")));
        linhasPrincipais.append(linha(List.of(
                "2.672", "GIZ DE GESSO COMUM", "", "96099000", "UN", "DELTA GIZ",
                "001000110000001", "491,000", "2,7072", "0,0000", "1.329,24"
        )));
        linhasPrincipais.append(linha(List.of("001.003.0006.0002 - GIZ DE CERA")));
        linhasPrincipais.append(linha(List.of(
                "819.971", "BIG GIZ CERA", "", "96099000", "UN", "LEONORA",
                "72304", "0,000", "24,8800", "0,0000", "0,00"
        )));
        linhasPrincipais.append(linha(List.of(
                "TOTAL DO INVENTÁRIO ESCRITO POR EXTENSO EM UMA LINHA QUE ULTRAPASSA SETENTA E CINCO CARACTERES"
        )));

        StringBuilder linhasContinuacao = new StringBuilder();
        linhasContinuacao.append(linha(List.of(
                "800.068", "BIG GIZ CERA TRIANGULAR", "", "96099000", "PCT", "ACRILEX",
                "", "12,000", "0,0001", "0,0000", "0,00"
        )));

        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <office:document-content
                    xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
                    xmlns:table="urn:oasis:names:tc:opendocument:xmlns:table:1.0"
                    xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0">
                  <office:body><office:spreadsheet>
                    <table:table table:name="Inventário">%s</table:table>
                    <table:table table:name="Inventário_1">%s</table:table>
                  </office:spreadsheet></office:body>
                </office:document-content>
                """.formatted(linhasPrincipais, linhasContinuacao);
        return empacotarOds(xml.getBytes(StandardCharsets.UTF_8));
    }

    private byte[] odsPromocoesExemplo() throws Exception {
        List<String> cabecalhos = List.of(
                "Código", "Nome", "Marca", "Código original", "Und", "Custo",
                "Preço venda", "Preço venda com índice", "Qtd máx", "Qtd máx. por cliente",
                "Data inicial", "Data final", "% Margem", "% Desconto", "Preço promoção",
                "Especial", "Desat. prom."
        );
        StringBuilder linhas = new StringBuilder();
        linhas.append(linha(cabecalhos));
        linhas.append(linha(List.of("001 - FERRAMENTAS")));
        linhas.append(linha(List.of(
                "838.563", "PRODUTO A", "MARCA A", "", "UN", "50,00", "100,00", "100,00",
                "0", "0", "14/08/2026", "30/08/2026", "44,38", "10,10", "89,90", "Não", "Não"
        )));
        linhas.append(linha(List.of(
                "48.340", "PRODUTO B", "MARCA B", "", "UN", "10,00", "20,00", "20,00",
                "0", "0", "14/08/2026", "30/08/2026", "", "", "15,00", "Não", "Sim"
        )));
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <office:document-content
                    xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
                    xmlns:table="urn:oasis:names:tc:opendocument:xmlns:table:1.0"
                    xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0">
                  <office:body><office:spreadsheet><table:table table:name="Promoções de Venda">
                    %s
                  </table:table></office:spreadsheet></office:body>
                </office:document-content>
                """.formatted(linhas);
        return empacotarOds(xml.getBytes(StandardCharsets.UTF_8));
    }

    private byte[] empacotarOds(byte[] contentXml) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes, StandardCharsets.UTF_8)) {
            zip.putNextEntry(new ZipEntry("mimetype"));
            zip.write("application/vnd.oasis.opendocument.spreadsheet".getBytes(
                    StandardCharsets.UTF_8
            ));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("content.xml"));
            zip.write(contentXml);
            zip.closeEntry();
        }
        return bytes.toByteArray();
    }

    private List<String> produto(String codigo, String ativo, String estoque) {
        List<String> valores = new ArrayList<>(Arrays.asList(new String[57]));
        valores.replaceAll(ignorado -> "");
        valores.set(0, codigo);
        valores.set(1, "GIZ DE GESSO COMUM");
        valores.set(2, "96099000");
        valores.set(3, "GIZ DE GESSO C/64 UN");
        valores.set(4, "24.316 - DELTA INDUSTRIA E COMERCIO DE GIZ LTDA");
        valores.set(5, "DELTA GIZ");
        valores.set(6, ativo);
        valores.set(7, "UN");
        valores.set(8, "CX");
        valores.set(11, "01/08/2019");
        valores.set(12, "001000110000001");
        valores.set(13, "7897464700019");
        valores.set(14, "Não");
        valores.set(19, estoque);
        valores.set(20, "5,49");
        valores.set(33, "13,00");
        valores.set(34, "0,015833");
        valores.set(52, "0 - Nacional");
        valores.set(53, "Sim");
        valores.set(54, "Não");
        valores.set(55, "100,00");
        valores.set(56, "18453F74-9616-471D-95F1-453C6B3C5869");
        return valores;
    }

    private String linha(List<String> valores) {
        StringBuilder linha = new StringBuilder("<table:table-row>");
        for (String valor : valores) {
            linha.append("<table:table-cell><text:p>")
                    .append(escaparXml(valor))
                    .append("</text:p></table:table-cell>");
        }
        return linha.append("</table:table-row>").toString();
    }

    private String escaparXml(String valor) {
        return valor
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
