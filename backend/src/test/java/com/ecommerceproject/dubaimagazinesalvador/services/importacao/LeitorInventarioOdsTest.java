package com.ecommerceproject.dubaimagazinesalvador.services.importacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.ecommerceproject.dubaimagazinesalvador.domain.importacao.InventarioOdsDTO;
import com.ecommerceproject.dubaimagazinesalvador.domain.produto.ProdutoImportacaoDTO;

class LeitorInventarioOdsTest {

    private final LeitorInventarioOds leitor = new LeitorInventarioOds();

    @Test
    void deveLerCategoriasHierarquicasEProdutoComNumerosBrasileiros() throws Exception {
        InventarioOdsDTO inventario = leitor.ler(new ByteArrayInputStream(odsExemplo()));

        assertEquals(4, inventario.categorias().size());
        assertEquals("PAPELARIA > ESCOLAR > COLORIR > GIZ DE CERA",
                inventario.categorias().get(3).caminho());

        ProdutoImportacaoDTO produto = inventario.produtos().getFirst();
        assertEquals("2672", produto.codigoSantri());
        assertEquals("001.003.0006.0002", produto.categoriaCodigo());
        assertEquals(new BigDecimal("525.000"), produto.quantidade());
        assertEquals(new BigDecimal("5.49"), produto.precoVenda());
        assertEquals(0, BigDecimal.ZERO.compareTo(produto.precoVendaIva()));
        assertEquals("001000110000001", produto.codigoOriginal());
    }

    @Test
    void deveLerOInventarioRealQuandoCaminhoForInformado() throws Exception {
        String caminho = System.getProperty("ods.file");
        Assumptions.assumeTrue(caminho != null && Files.exists(Path.of(caminho)));

        InventarioOdsDTO inventario;
        try (FileInputStream input = new FileInputStream(caminho)) {
            inventario = leitor.ler(input);
        }

        assertEquals(2_757, inventario.categorias().size());
        assertEquals(54_081, inventario.produtos().size());
        assertTrue(inventario.produtos().stream()
                .allMatch(produto -> produto.quantidade().signum() >= 0));
    }

    private byte[] odsExemplo() throws Exception {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <office:document-content
                    xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
                    xmlns:table="urn:oasis:names:tc:opendocument:xmlns:table:1.0"
                    xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0">
                  <office:body>
                    <office:spreadsheet>
                      <table:table table:name="Inventário">
                        <table:table-row>
                          <table:table-cell><text:p>Produto</text:p></table:table-cell>
                          <table:table-cell table:number-columns-spanned="2"><text:p>Descrição</text:p></table:table-cell>
                          <table:covered-table-cell/>
                          <table:table-cell><text:p>Cód. fiscal</text:p></table:table-cell>
                          <table:table-cell><text:p>Und</text:p></table:table-cell>
                          <table:table-cell><text:p>Marca</text:p></table:table-cell>
                          <table:table-cell><text:p>Cód. Original</text:p></table:table-cell>
                          <table:table-cell><text:p>Quantidade</text:p></table:table-cell>
                          <table:table-cell><text:p>Pço Venda</text:p></table:table-cell>
                          <table:table-cell><text:p>Pço Vend(IVA)</text:p></table:table-cell>
                          <table:table-cell><text:p>Total</text:p></table:table-cell>
                        </table:table-row>
                        %s
                        %s
                        %s
                        %s
                        <table:table-row>
                          <table:table-cell><text:p>2.672</text:p></table:table-cell>
                          <table:table-cell table:number-columns-spanned="2">
                            <text:p>GIZ DE GESSO COMUM</text:p>
                          </table:table-cell>
                          <table:covered-table-cell/>
                          <table:table-cell><text:p>96099000</text:p></table:table-cell>
                          <table:table-cell><text:p>UN</text:p></table:table-cell>
                          <table:table-cell><text:p>DELTA GIZ</text:p></table:table-cell>
                          <table:table-cell><text:p>001000110000001</text:p></table:table-cell>
                          <table:table-cell><text:p>525,000</text:p></table:table-cell>
                          <table:table-cell><text:p>5,49</text:p></table:table-cell>
                          <table:table-cell><text:p>0,00</text:p></table:table-cell>
                          <table:table-cell><text:p>2.882,25</text:p></table:table-cell>
                        </table:table-row>
                      </table:table>
                    </office:spreadsheet>
                  </office:body>
                </office:document-content>
                """.formatted(
                linhaCategoria("001 - PAPELARIA"),
                linhaCategoria("001.003 - ESCOLAR"),
                linhaCategoria("001.003.0006 - COLORIR"),
                linhaCategoria("001.003.0006.0002 - GIZ DE CERA")
        );

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes, StandardCharsets.UTF_8)) {
            zip.putNextEntry(new ZipEntry("content.xml"));
            zip.write(xml.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return bytes.toByteArray();
    }

    private String linhaCategoria(String categoria) {
        return """
                <table:table-row>
                  <table:table-cell table:number-columns-spanned="11">
                    <text:p>%s</text:p>
                  </table:table-cell>
                  <table:covered-table-cell table:number-columns-repeated="10"/>
                </table:table-row>
                """.formatted(categoria);
    }
}
