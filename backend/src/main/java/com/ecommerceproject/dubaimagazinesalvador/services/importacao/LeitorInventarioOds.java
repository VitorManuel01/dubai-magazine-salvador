package com.ecommerceproject.dubaimagazinesalvador.services.importacao;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.springframework.stereotype.Component;

import com.ecommerceproject.dubaimagazinesalvador.domain.importacao.CategoriaImportacaoDTO;
import com.ecommerceproject.dubaimagazinesalvador.domain.importacao.InventarioOdsDTO;
import com.ecommerceproject.dubaimagazinesalvador.domain.produto.ProdutoImportacaoDTO;

@Component
public class LeitorInventarioOds {

    private static final String TABLE_NS = "urn:oasis:names:tc:opendocument:xmlns:table:1.0";
    private static final String TEXT_NS = "urn:oasis:names:tc:opendocument:xmlns:text:1.0";
    private static final Pattern CATEGORIA_PATTERN =
            Pattern.compile("^(\\d+(?:\\.\\d+){0,3})\\s+-\\s+(.+)$");
    private static final int MAX_COLUNAS_LIDAS = 32;
    private static final int MAX_LINHAS = 500_000;
    private static final long MAX_CONTENT_XML_BYTES = 250L * 1024L * 1024L;

    public InventarioOdsDTO ler(InputStream arquivo) {
        try (ZipInputStream zip = new ZipInputStream(
                new BufferedInputStream(arquivo),
                StandardCharsets.UTF_8
        )) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (!"content.xml".equals(entry.getName())) {
                    continue;
                }
                if (entry.getSize() > MAX_CONTENT_XML_BYTES) {
                    throw new ImportacaoOdsException("O conteúdo descompactado do ODS é muito grande.");
                }
                return lerContentXml(zip);
            }
        } catch (IOException e) {
            throw new ImportacaoOdsException("Não foi possível abrir o arquivo ODS.", e);
        }

        throw new ImportacaoOdsException("Arquivo inválido: content.xml não encontrado no ODS.");
    }

    private InventarioOdsDTO lerContentXml(InputStream contentXml) {
        XMLInputFactory factory = XMLInputFactory.newFactory();
        configurarXmlSeguro(factory);

        try {
            XMLStreamReader reader = factory.createXMLStreamReader(contentXml, StandardCharsets.UTF_8.name());
            try {
                return percorrerPlanilha(reader);
            } finally {
                reader.close();
            }
        } catch (XMLStreamException e) {
            throw new ImportacaoOdsException("O conteúdo XML do ODS está inválido.", e);
        }
    }

    private InventarioOdsDTO percorrerPlanilha(XMLStreamReader reader) throws XMLStreamException {
        Map<String, CategoriaImportacaoDTO> categorias = new LinkedHashMap<>();
        Map<String, ProdutoImportacaoDTO> produtos = new LinkedHashMap<>();
        boolean dentroDaPrimeiraPlanilha = false;
        boolean primeiraPlanilhaEncerrada = false;
        boolean cabecalhoEncontrado = false;
        String categoriaAtual = null;
        int numeroLinha = 0;
        int linhasIgnoradas = 0;

        while (reader.hasNext() && !primeiraPlanilhaEncerrada) {
            int event = reader.next();

            if (event == XMLStreamConstants.START_ELEMENT
                    && elemento(reader, TABLE_NS, "table")
                    && !dentroDaPrimeiraPlanilha) {
                dentroDaPrimeiraPlanilha = true;
                continue;
            }

            if (event == XMLStreamConstants.END_ELEMENT
                    && elemento(reader, TABLE_NS, "table")
                    && dentroDaPrimeiraPlanilha) {
                primeiraPlanilhaEncerrada = true;
                continue;
            }

            if (event != XMLStreamConstants.START_ELEMENT
                    || !elemento(reader, TABLE_NS, "table-row")
                    || !dentroDaPrimeiraPlanilha) {
                continue;
            }

            numeroLinha++;
            if (numeroLinha > MAX_LINHAS) {
                throw new ImportacaoOdsException("O ODS ultrapassa o limite de linhas permitido.");
            }

            List<String> colunas = lerLinha(reader);
            if (ehCabecalho(colunas)) {
                cabecalhoEncontrado = true;
                continue;
            }
            if (!cabecalhoEncontrado || linhaVazia(colunas)) {
                continue;
            }

            String primeiraColuna = valor(colunas, 0);
            Matcher categoriaMatcher = CATEGORIA_PATTERN.matcher(primeiraColuna);
            if (categoriaMatcher.matches()) {
                CategoriaImportacaoDTO categoria = montarCategoria(
                        categoriaMatcher.group(1),
                        categoriaMatcher.group(2),
                        categorias,
                        numeroLinha
                );
                categorias.put(categoria.codigo(), categoria);
                categoriaAtual = categoria.codigo();
                continue;
            }

            String codigoProduto = normalizarCodigoProduto(primeiraColuna);
            if (codigoProduto == null) {
                linhasIgnoradas++;
                continue;
            }
            if (categoriaAtual == null) {
                throw erroLinha(numeroLinha, "produto encontrado antes de qualquer categoria.");
            }

            ProdutoImportacaoDTO produto = montarProduto(
                    colunas,
                    codigoProduto,
                    categoriaAtual,
                    numeroLinha
            );
            produtos.put(codigoProduto, produto);
        }

        if (!cabecalhoEncontrado) {
            throw new ImportacaoOdsException(
                    "A planilha não possui o cabeçalho esperado: Produto, Descrição e Cód. fiscal."
            );
        }
        if (categorias.isEmpty() || produtos.isEmpty()) {
            throw new ImportacaoOdsException("Nenhuma categoria ou produto foi encontrado no ODS.");
        }

        return new InventarioOdsDTO(
                List.copyOf(categorias.values()),
                List.copyOf(produtos.values()),
                linhasIgnoradas
        );
    }

    private CategoriaImportacaoDTO montarCategoria(
            String codigo,
            String nome,
            Map<String, CategoriaImportacaoDTO> categorias,
            int numeroLinha
    ) {
        String[] partes = codigo.split("\\.");
        int nivel = partes.length;
        if (nivel < 1 || nivel > 4) {
            throw erroLinha(numeroLinha, "nível de categoria inválido: " + codigo);
        }

        String codigoPai = nivel == 1
                ? null
                : codigo.substring(0, codigo.lastIndexOf('.'));
        CategoriaImportacaoDTO categoriaPai = codigoPai == null ? null : categorias.get(codigoPai);
        if (codigoPai != null && categoriaPai == null) {
            throw erroLinha(numeroLinha, "categoria pai não encontrada para " + codigo);
        }

        String nomeNormalizado = normalizarTexto(nome);
        String caminho = categoriaPai == null
                ? nomeNormalizado
                : categoriaPai.caminho() + " > " + nomeNormalizado;

        return new CategoriaImportacaoDTO(
                codigo,
                nomeNormalizado,
                nivel,
                caminho,
                codigoPai
        );
    }

    private ProdutoImportacaoDTO montarProduto(
            List<String> colunas,
            String codigoProduto,
            String categoriaAtual,
            int numeroLinha
    ) {
        if (colunas.size() < 10) {
            throw erroLinha(numeroLinha, "linha de produto incompleta.");
        }

        String descricao = normalizarTexto(valor(colunas, 1));
        if (descricao.isBlank()) {
            throw erroLinha(numeroLinha, "produto " + codigoProduto + " sem descrição.");
        }

        return new ProdutoImportacaoDTO(
                codigoProduto,
                descricao,
                textoOpcional(valor(colunas, 3)),
                textoOpcional(valor(colunas, 4)),
                textoOpcional(valor(colunas, 5)),
                textoOpcional(valor(colunas, 6)),
                decimalBrasileiro(valor(colunas, 7), numeroLinha, "quantidade"),
                decimalBrasileiro(valor(colunas, 8), numeroLinha, "preço de venda"),
                decimalBrasileiro(valor(colunas, 9), numeroLinha, "preço de venda com IVA"),
                categoriaAtual
        );
    }

    private List<String> lerLinha(XMLStreamReader reader) throws XMLStreamException {
        List<String> colunas = new ArrayList<>(12);

        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.END_ELEMENT
                    && elemento(reader, TABLE_NS, "table-row")) {
                return colunas;
            }
            if (event != XMLStreamConstants.START_ELEMENT) {
                continue;
            }

            if (elemento(reader, TABLE_NS, "table-cell")) {
                int repeticoes = repeticoes(reader);
                String conteudo = lerCelula(reader);
                adicionarColunas(colunas, conteudo, repeticoes);
            } else if (elemento(reader, TABLE_NS, "covered-table-cell")) {
                adicionarColunas(colunas, "", repeticoes(reader));
            }
        }

        return colunas;
    }

    private String lerCelula(XMLStreamReader reader) throws XMLStreamException {
        StringBuilder texto = new StringBuilder();
        int profundidade = 1;
        int dentroDeParagrafo = 0;

        while (reader.hasNext() && profundidade > 0) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                profundidade++;
                if (elemento(reader, TEXT_NS, "p")) {
                    if (!texto.isEmpty()) {
                        texto.append(' ');
                    }
                    dentroDeParagrafo++;
                }
            } else if (event == XMLStreamConstants.CHARACTERS && dentroDeParagrafo > 0) {
                texto.append(reader.getText());
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (elemento(reader, TEXT_NS, "p")) {
                    dentroDeParagrafo--;
                }
                profundidade--;
            }
        }

        return normalizarTexto(texto.toString());
    }

    private int repeticoes(XMLStreamReader reader) {
        String valor = reader.getAttributeValue(TABLE_NS, "number-columns-repeated");
        if (valor == null || valor.isBlank()) {
            return 1;
        }
        try {
            return Math.max(1, Integer.parseInt(valor));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private void adicionarColunas(List<String> colunas, String valor, int repeticoes) {
        int quantidade = Math.min(repeticoes, MAX_COLUNAS_LIDAS - colunas.size());
        for (int i = 0; i < quantidade; i++) {
            colunas.add(valor);
        }
    }

    private boolean ehCabecalho(List<String> colunas) {
        return "produto".equalsIgnoreCase(valor(colunas, 0))
                && "descrição".equalsIgnoreCase(valor(colunas, 1))
                && normalizarTexto(valor(colunas, 3)).toLowerCase().startsWith("cód");
    }

    private boolean linhaVazia(List<String> colunas) {
        return colunas.stream().allMatch(String::isBlank);
    }

    private String normalizarCodigoProduto(String codigo) {
        String normalizado = codigo.replace(".", "").replaceAll("\\s+", "");
        return normalizado.matches("\\d+") ? normalizado : null;
    }

    private BigDecimal decimalBrasileiro(String valor, int numeroLinha, String campo) {
        String normalizado = valor
                .replace("\u00A0", "")
                .replace(" ", "")
                .replace(".", "")
                .replace(",", ".");
        if (normalizado.isBlank() || "-".equals(normalizado)) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(normalizado);
        } catch (NumberFormatException e) {
            throw erroLinha(numeroLinha, campo + " inválido: " + valor);
        }
    }

    private String textoOpcional(String valor) {
        String normalizado = normalizarTexto(valor);
        return normalizado.isBlank() ? null : normalizado;
    }

    private String normalizarTexto(String valor) {
        return valor == null ? "" : valor.trim().replaceAll("\\s+", " ");
    }

    private String valor(List<String> colunas, int indice) {
        return indice < colunas.size() ? colunas.get(indice) : "";
    }

    private boolean elemento(XMLStreamReader reader, String namespace, String nomeLocal) {
        return namespace.equals(reader.getNamespaceURI()) && nomeLocal.equals(reader.getLocalName());
    }

    private void configurarXmlSeguro(XMLInputFactory factory) {
        try {
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        } catch (IllegalArgumentException ignored) {
            // Implementações StAX podem não expor esta propriedade.
        }
        try {
            factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
        } catch (IllegalArgumentException ignored) {
            // Implementações StAX podem não expor esta propriedade.
        }
    }

    private ImportacaoOdsException erroLinha(int numeroLinha, String detalhe) {
        return new ImportacaoOdsException("Linha " + numeroLinha + ": " + detalhe);
    }
}
