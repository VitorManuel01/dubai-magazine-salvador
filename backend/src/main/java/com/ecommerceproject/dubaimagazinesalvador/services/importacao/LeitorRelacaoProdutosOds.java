package com.ecommerceproject.dubaimagazinesalvador.services.importacao;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.springframework.stereotype.Component;

import com.ecommerceproject.dubaimagazinesalvador.domain.importacao.CategoriaImportacaoDTO;
import com.ecommerceproject.dubaimagazinesalvador.domain.importacao.RelacaoProdutosOdsDTO;
import com.ecommerceproject.dubaimagazinesalvador.domain.produto.ProdutoImportacaoDTO;

@Component
public class LeitorRelacaoProdutosOds {

    private static final String TABLE_NS = "urn:oasis:names:tc:opendocument:xmlns:table:1.0";  //identificador do namespace para elementos de tabela no ODS
    private static final String TEXT_NS = "urn:oasis:names:tc:opendocument:xmlns:text:1.0"; //identificador do namespace para elementos de texto no ODS
    private static final Pattern CODIGO_CATEGORIA =
            Pattern.compile("^\\d+(?:\\.\\d+){0,3}$"); // expressão regular para validar códigos de categoria no formato "1", "1.2", "1.2.3" ou "
    private static final Pattern CODIGO_PRODUTO = Pattern.compile("^[\\d.]+$"); // expressão regular para validar códigos de produto que consistem apenas em dígitos e pontos
    private static final DateTimeFormatter DATA_BRASILEIRA = DateTimeFormatter
            .ofPattern("dd/MM/uuuu", Locale.ROOT)
            .withResolverStyle(ResolverStyle.STRICT); //formato de data brasileiro (dia/mês/ano) com validação rigorosa
    private static final int MAX_COLUNAS_LIDAS = 80; // máximo de colunas que serão lidas de cada linha da planilha ODS
    private static final int MAX_LINHAS = 25_000; // máximo de linhas que serão lidas da planilha ODS
    private static final int MAX_PRODUTOS = 25_000; // máximo de produtos únicos aceitos em uma importação
    private static final int MAX_CARACTERES_CELULA = 75; // máximo de caracteres Unicode por célula
    private static final int MAX_ENTRADAS_ZIP = 100; // máximo de entradas (arquivos internos) que serão lidas do arquivo ODS compactado (ZIP)
    private static final long MAX_ARQUIVO_BYTES = 20L * 1024L * 1024L; // tamanho máximo do arquivo ODS que será aceito (20 MB)
    private static final long MAX_CONTENT_XML_BYTES = 250L * 1024L * 1024L; // tamanho máximo do arquivo content.xml dentro do ODS que será aceito (250 MB)
    private static final long MAX_TOTAL_DESCOMPACTADO_BYTES = 300L * 1024L * 1024L; // tamanho máximo total descompactado de todos os arquivos internos do ODS que será aceito (300 MB)
    private static final long TAMANHO_MINIMO_PARA_RAZAO = 1024L * 1024L;// tamanho mínimo de um arquivo interno do ODS para que seja verificada a razão entre o tamanho compactado e descompactado (1 MB)
    private static final double RAZAO_MINIMA_COMPACTACAO = 0.01d; // razão mínima entre o tamanho compactado e descompactado de um arquivo interno do ODS para que seja aceito (1%)
    private static final String MIME_ODS =
            "application/vnd.oasis.opendocument.spreadsheet"; // tipo MIME que identifica arquivos ODS (OpenDocument Spreadsheet)

    private static final List<String> CABECALHOS_OBRIGATORIOS = List.of( //cabeçalhos obrigatórios que devem estar presentes na planilha ODS para que seja considerada válida
            "codigo",
            "nome",
            "ncm",
            "nome de compra",
            "fabricante",
            "marca",
            "ativo",
            "und venda",
            "und compra",
            "data cadastro",
            "codigo original",
            "codigo de barras",
            "bloqueado para compras",
            "estoque",
            "preco",
            "ipi entrada",
            "peso da unidade",
            "altura da unidade",
            "largura da unidade",
            "comprimento da unidade",
            "volume da unidade m3",
            "volume em litros",
            "peso da caixa",
            "altura da caixa",
            "largura da caixa",
            "comprimento da caixa",
            "origem",
            "industrializado",
            "insumo",
            "max aprov ipi",
            "numero fci"
    );

    public RelacaoProdutosOdsDTO ler(InputStream arquivo) { // lê o arquivo ODS a partir de um InputStream, processa seu conteúdo e retorna um objeto RelacaoProdutosOdsDTO que contém as categorias e produtos extraídos da planilha
        Path temporario = null; // este caminho temporário é usado para armazenar o arquivo ODS enquanto ele é processado, garantindo que o arquivo seja fechado e excluído corretamente após a leitura
        try {
            temporario = copiarParaTemporario(arquivo);
            return lerArquivoZip(temporario);
        } catch (ImportacaoOdsException e) {
            throw e;
        } catch (IOException e) {
            throw new ImportacaoOdsException("Não foi possível abrir o arquivo ODS.", e);
        } finally {
            apagarTemporario(temporario);
        }
    }

    private RelacaoProdutosOdsDTO lerArquivoZip(Path arquivo) throws IOException { // lê o arquivo ODS compactado (ZIP) a partir de um caminho temporário, valida sua estrutura e conteúdo, e retorna um objeto RelacaoProdutosOdsDTO que contém as categorias e produtos extraídos da planilha
        try (ZipFile zip = new ZipFile(arquivo.toFile(), StandardCharsets.UTF_8)) {
            ZipEntry contentXml = validarEstruturaZip(zip);
            validarMimetype(zip);
            try (InputStream content = new InputStreamLimitado(
                    zip.getInputStream(contentXml),
                    MAX_CONTENT_XML_BYTES
            )) {
                return lerContentXml(content);
            }
        }
    }

/*
 * Valida restrições de segurança da estrutura ZIP do arquivo ODS:
 * quantidade de entradas, nomes internos, duplicidades, tamanhos,
 * volume total descompactado e razão de compactação.
 *
 * Também localiza e retorna a entrada content.xml.
 *
 * Este método não valida sozinho toda a conformidade com o formato ODS;
 * o mimetype e o conteúdo XML são validados em etapas posteriores.
 */
    private ZipEntry validarEstruturaZip(ZipFile zip) {
        Enumeration<? extends ZipEntry> entradas = zip.entries(); // enumerador para percorrer todas as entradas (arquivos internos) do arquivo ZIP
        Set<String> nomes = new HashSet<>(); // conjunto para armazenar os nomes das entradas do arquivo ZIP, garantindo que não haja duplicatas
        ZipEntry contentXml = null; //o content.xml é o arquivo principal dentro do ODS que contém os dados da planilha, e será armazenado nesta variável para posterior leitura e processamento
        int quantidade = 0; // contador para rastrear o número de entradas processadas no arquivo ZIP, garantindo que não ultrapasse o limite máximo permitido
        long totalDescompactado = 0; // acumulador para rastrear o tamanho total descompactado de todas as entradas processadas no arquivo ZIP, garantindo que não ultrapasse o limite máximo permitido

        while (entradas.hasMoreElements()) {//enquanto houver entradas no arquivo ZIP, o loop continua, se passar do máximo para
            ZipEntry entrada = entradas.nextElement();
            quantidade++;
            if (quantidade > MAX_ENTRADAS_ZIP) {
                throw new ImportacaoOdsException("O ODS contém arquivos internos demais.");
            }
            validarNomeEntrada(entrada.getName());
            if (!nomes.add(entrada.getName())) {
                throw new ImportacaoOdsException("O ODS contém arquivos internos duplicados.");
            }
            if (entrada.isDirectory()) {
                continue;
            }

            long tamanho = entrada.getSize(); // verifica o tamanho descompactado do arquivo interno, se for negativo é inválido
            long compactado = entrada.getCompressedSize(); // verifica o tamanho compactado do arquivo interno, se for negativo é inválido
            if (tamanho < 0 || compactado < 0) {
                throw new ImportacaoOdsException("O ODS possui tamanho interno inválido.");
            }
            if (tamanho > MAX_CONTENT_XML_BYTES) {
                throw new ImportacaoOdsException(
                        "Um arquivo interno do ODS excede o limite permitido."
                );
            }
            totalDescompactado += tamanho; // verifica o tamanho total descompactado de todas as entradas processadas, se ultrapassar o limite máximo permitido, lança uma exceção
            if (totalDescompactado > MAX_TOTAL_DESCOMPACTADO_BYTES) {
                throw new ImportacaoOdsException(
                        "O conteúdo descompactado do ODS é muito grande."
                );
            }
            if (tamanho >= TAMANHO_MINIMO_PARA_RAZAO //verifica a razão entre o tamanho compactado e descompactado de um arquivo interno do ODS, se for menor que a razão mínima permitida, lança uma exceção
                    && compactado > 0
                    && (double) compactado / tamanho < RAZAO_MINIMA_COMPACTACAO) {
                throw new ImportacaoOdsException(
                        "O ODS foi rejeitado por apresentar compactação suspeita."
                );
            }
            if ("content.xml".equals(entrada.getName())) {
                contentXml = entrada;
            }
        }

        if (contentXml == null) {
            throw new ImportacaoOdsException(
                    "Arquivo inválido: content.xml não encontrado no ODS."
            );
        }
        return contentXml;
    }

    // Valida o tipo MIME do arquivo ZIP, garantindo que ele seja um ODS válido.
    private void validarMimetype(ZipFile zip) throws IOException {
        ZipEntry mimetype = zip.getEntry("mimetype");// verifica se o arquivo ZIP contém a entrada "mimetype", que é um arquivo especial dentro do ODS que indica o tipo MIME do arquivo, e se o tamanho do arquivo "mimetype" não ultrapassa 200 bytes, garantindo que ele seja pequeno e seguro para leitura
        if (mimetype == null || mimetype.getSize() > 200) { //invalida o arquivo ODS se a entrada "mimetype" não estiver presente ou se o tamanho do arquivo "mimetype" for maior que 200 bytes, lançando uma exceção ImportacaoOdsException com uma mensagem de erro apropriada
            throw new ImportacaoOdsException("Arquivo inválido: tipo ODS não confirmado.");
        }
        try (InputStream input = new InputStreamLimitado(zip.getInputStream(mimetype), 200)) {
            String valor = new String(input.readAllBytes(), StandardCharsets.UTF_8).trim();//tenta ler o conteúdo do arquivo "mimetype" e verificar se ele corresponde ao tipo MIME esperado para arquivos ODS, que é "application/vnd.oasis.opendocument.spreadsheet". Se o conteúdo do arquivo "mimetype" não corresponder a esse valor, lança uma exceção ImportacaoOdsException com uma mensagem de erro apropriada
            if (!MIME_ODS.equals(valor)) { 
                throw new ImportacaoOdsException("Arquivo inválido: tipo ODS não confirmado.");
            }
        }
    }

    private Path copiarParaTemporario(InputStream arquivo) throws IOException {
        Path temporario = Files.createTempFile("importacao-produtos-", ".ods");
        boolean concluido = false;
        try (OutputStream output = Files.newOutputStream(temporario)) {
            byte[] buffer = new byte[8192];
            long total = 0;
            int lidos;
            while ((lidos = arquivo.read(buffer)) != -1) {
                total += lidos;
                if (total > MAX_ARQUIVO_BYTES) {
                    throw new ImportacaoOdsException("O arquivo excede o limite de 20 MB.");
                }
                output.write(buffer, 0, lidos);
            }
            concluido = true;
            return temporario;
        } finally {
            if (!concluido) {
                Files.deleteIfExists(temporario);
            }
        }
    }

    private void validarNomeEntrada(String nome) {
        String normalizado = nome.replace('\\', '/');
        if (normalizado.startsWith("/")
                || normalizado.contains(":")
                || List.of(normalizado.split("/")).contains("..")) {
            throw new ImportacaoOdsException("O ODS possui caminho interno inválido.");
        }
    }

    private void apagarTemporario(Path temporario) {
        if (temporario == null) {
            return;
        }
        try {
            Files.deleteIfExists(temporario);
        } catch (IOException ignored) {
            // O sistema operacional remove seus arquivos temporários posteriormente.
        }
    }

    private RelacaoProdutosOdsDTO lerContentXml(InputStream contentXml) {
        XMLInputFactory factory = XMLInputFactory.newFactory();
        configurarXmlSeguro(factory);

        try {
            XMLStreamReader reader = factory.createXMLStreamReader(
                    contentXml,
                    StandardCharsets.UTF_8.name()
            );
            try {
                return percorrerPlanilha(reader);
            } finally {
                reader.close();
            }
        } catch (XMLStreamException e) {
            throw new ImportacaoOdsException("O conteúdo XML do ODS está inválido.", e);
        }
    }

    private RelacaoProdutosOdsDTO percorrerPlanilha(XMLStreamReader reader)
            throws XMLStreamException {
        Map<String, CategoriaImportacaoDTO> categorias = new LinkedHashMap<>();
        Map<String, ProdutoImportacaoDTO> produtos = new LinkedHashMap<>();
        Map<String, Integer> cabecalhos = null;
        boolean dentroDaPrimeiraPlanilha = false;
        boolean primeiraPlanilhaEncerrada = false;
        String categoriaAtual = null;
        int numeroLinha = 0;
        int linhasIgnoradas = 0;

        while (reader.hasNext() && !primeiraPlanilhaEncerrada) {
            int event = reader.next();

            if (event == XMLStreamConstants.DTD
                    || event == XMLStreamConstants.ENTITY_REFERENCE) {
                throw new ImportacaoOdsException(
                        "O conteúdo XML do ODS contém DTD ou entidade, o que não é permitido."
                );
            }

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
                throw new ImportacaoOdsException(
                        "O ODS ultrapassa o limite de 25.000 linhas."
                );
            }

            List<String> colunas = lerLinha(reader, numeroLinha);
            if (cabecalhos == null) {
                Map<String, Integer> possiveisCabecalhos = mapearCabecalhos(colunas);
                if (ehCabecalhoAnalitico(possiveisCabecalhos)) {
                    validarCabecalhos(possiveisCabecalhos);
                    cabecalhos = possiveisCabecalhos;
                }
                continue;
            }
            if (linhaVazia(colunas)) {
                continue;
            }

            String codigoInformado = campo(colunas, cabecalhos, "codigo");
            if (ehLinhaCategoria(colunas, cabecalhos, codigoInformado)) {
                CategoriaImportacaoDTO categoria = montarCategoria(
                        codigoInformado,
                        campo(colunas, cabecalhos, "nome"),
                        categorias,
                        numeroLinha
                );
                categorias.put(categoria.codigo(), categoria);
                categoriaAtual = categoria.codigo();
                continue;
            }

            String codigoProduto = normalizarCodigoProduto(codigoInformado);
            if (codigoProduto == null) {
                linhasIgnoradas++;
                continue;
            }
            if (categoriaAtual == null) {
                throw erroLinha(numeroLinha, "produto encontrado antes de qualquer categoria.");
            }

            Boolean ativo = booleanoOpcional(
                    campo(colunas, cabecalhos, "ativo"),
                    numeroLinha,
                    "Ativo?"
            );
            BigDecimal estoque = decimalOpcional(
                    campo(colunas, cabecalhos, "estoque"),
                    numeroLinha,
                    "Estoque"
            );
            if (!Boolean.TRUE.equals(ativo)
                    || estoque == null
                    || estoque.signum() <= 0) {
                linhasIgnoradas++;
                continue;
            }

            ProdutoImportacaoDTO produto = montarProduto(
                    colunas,
                    cabecalhos,
                    codigoProduto,
                    categoriaAtual,
                    ativo,
                    estoque,
                    numeroLinha
            );
            produtos.put(codigoProduto, produto);
            if (produtos.size() > MAX_PRODUTOS) {
                throw new ImportacaoOdsException(
                        "O ODS ultrapassa o limite de 25.000 produtos."
                );
            }
        }

        if (cabecalhos == null) {
            throw new ImportacaoOdsException(
                    "A planilha não possui o cabeçalho da Relação de Produtos por Grupo."
            );
        }
        if (categorias.isEmpty() || produtos.isEmpty()) {
            throw new ImportacaoOdsException(
                    "Nenhuma categoria ou produto ativo com estoque positivo foi encontrado no ODS."
            );
        }

        return new RelacaoProdutosOdsDTO(
                List.copyOf(categorias.values()),
                List.copyOf(produtos.values()),
                linhasIgnoradas
        );
    }

    private ProdutoImportacaoDTO montarProduto(
            List<String> colunas,
            Map<String, Integer> cabecalhos,
            String codigoProduto,
            String categoriaAtual,
            boolean ativo,
            BigDecimal estoque,
            int numeroLinha
    ) {
        String nome = normalizarTexto(campo(colunas, cabecalhos, "nome"));
        if (nome.isBlank()) {
            throw erroLinha(numeroLinha, "produto " + codigoProduto + " sem nome.");
        }

        return new ProdutoImportacaoDTO(
                codigoProduto,
                nome,
                textoOpcional(campo(colunas, cabecalhos, "ncm")),
                textoOpcional(campo(colunas, cabecalhos, "nome de compra")),
                textoOpcional(campo(colunas, cabecalhos, "fabricante")),
                textoOpcional(campo(colunas, cabecalhos, "marca")),
                ativo,
                textoOpcional(campo(colunas, cabecalhos, "und venda")),
                textoOpcional(campo(colunas, cabecalhos, "und compra")),
                dataOpcional(
                        campo(colunas, cabecalhos, "data cadastro"),
                        numeroLinha
                ),
                textoOpcional(campo(colunas, cabecalhos, "codigo original")),
                textoOpcional(campo(colunas, cabecalhos, "codigo de barras")),
                Boolean.TRUE.equals(booleanoOpcional(
                        campo(colunas, cabecalhos, "bloqueado para compras"),
                        numeroLinha,
                        "Bloqueado para compras"
                )),
                estoque,
                decimalOuZero(campo(colunas, cabecalhos, "preco"), numeroLinha, "Preço"),
                decimalOpcional(
                        campo(colunas, cabecalhos, "ipi entrada"),
                        numeroLinha,
                        "% IPI entrada"
                ),
                decimalOpcional(campo(colunas, cabecalhos, "peso da unidade"), numeroLinha, "Peso da unidade"),
                decimalOpcional(campo(colunas, cabecalhos, "altura da unidade"), numeroLinha, "Altura da unidade"),
                decimalOpcional(campo(colunas, cabecalhos, "largura da unidade"), numeroLinha, "Largura da unidade"),
                decimalOpcional(campo(colunas, cabecalhos, "comprimento da unidade"), numeroLinha, "Comprimento da unidade"),
                decimalOpcional(campo(colunas, cabecalhos, "volume da unidade m3"), numeroLinha, "Volume da unidade (m³)"),
                decimalOpcional(campo(colunas, cabecalhos, "volume em litros"), numeroLinha, "Volume em litros"),
                decimalOpcional(campo(colunas, cabecalhos, "peso da caixa"), numeroLinha, "Peso da caixa"),
                decimalOpcional(campo(colunas, cabecalhos, "altura da caixa"), numeroLinha, "Altura da caixa"),
                decimalOpcional(campo(colunas, cabecalhos, "largura da caixa"), numeroLinha, "Largura da caixa"),
                decimalOpcional(campo(colunas, cabecalhos, "comprimento da caixa"), numeroLinha, "Comprimento da caixa"),
                textoOpcional(campo(colunas, cabecalhos, "origem")),
                booleanoOpcional(campo(colunas, cabecalhos, "industrializado"), numeroLinha, "Industrializado"),
                booleanoOpcional(campo(colunas, cabecalhos, "insumo"), numeroLinha, "Insumo"),
                decimalOpcional(campo(colunas, cabecalhos, "max aprov ipi"), numeroLinha, "% Máx. aprov. IPI"),
                textoOpcional(campo(colunas, cabecalhos, "numero fci")),
                categoriaAtual
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
        String codigoPai = nivel == 1
                ? null
                : codigo.substring(0, codigo.lastIndexOf('.'));
        CategoriaImportacaoDTO categoriaPai = codigoPai == null ? null : categorias.get(codigoPai);
        if (codigoPai != null && categoriaPai == null) {
            throw erroLinha(numeroLinha, "categoria pai não encontrada para " + codigo);
        }

        String nomeNormalizado = normalizarTexto(nome);
        if (nomeNormalizado.isBlank()) {
            throw erroLinha(numeroLinha, "categoria " + codigo + " sem nome.");
        }
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

    private boolean ehLinhaCategoria(
            List<String> colunas,
            Map<String, Integer> cabecalhos,
            String codigo
    ) {
        return CODIGO_CATEGORIA.matcher(codigo).matches()
                && campo(colunas, cabecalhos, "ativo").isBlank()
                && campo(colunas, cabecalhos, "estoque").isBlank()
                && campo(colunas, cabecalhos, "preco").isBlank();
    }

    private Map<String, Integer> mapearCabecalhos(List<String> colunas) {
        Map<String, Integer> cabecalhos = new LinkedHashMap<>();
        for (int indice = 0; indice < colunas.size(); indice++) {
            String chave = normalizarCabecalho(colunas.get(indice));
            if (!chave.isBlank()) {
                cabecalhos.putIfAbsent(chave, indice);
            }
        }
        return cabecalhos;
    }

    private boolean ehCabecalhoAnalitico(Map<String, Integer> cabecalhos) {
        return cabecalhos.containsKey("codigo")
                && cabecalhos.containsKey("nome")
                && cabecalhos.containsKey("estoque")
                && cabecalhos.containsKey("preco");
    }

    private void validarCabecalhos(Map<String, Integer> cabecalhos) {
        List<String> ausentes = CABECALHOS_OBRIGATORIOS.stream()
                .filter(cabecalho -> !cabecalhos.containsKey(cabecalho))
                .toList();
        if (!ausentes.isEmpty()) {
            throw new ImportacaoOdsException(
                    "A relação analítica não possui as colunas obrigatórias: "
                            + String.join(", ", ausentes)
                            + "."
            );
        }
    }

    private List<String> lerLinha(
            XMLStreamReader reader,
            int numeroLinha
    ) throws XMLStreamException {
        List<String> colunas = new ArrayList<>(60);

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
                String formula = reader.getAttributeValue(TABLE_NS, "formula");
                if (formula != null && !formula.isBlank()) {
                    throw new ImportacaoOdsException(
                            "O ODS contém fórmulas. Exporte somente valores antes da importação."
                    );
                }
                int repeticoes = repeticoes(reader);
                String conteudo = lerCelula(
                        reader,
                        numeroLinha,
                        colunas.size() + 1
                );
                adicionarColunas(colunas, conteudo, repeticoes);
            } else if (elemento(reader, TABLE_NS, "covered-table-cell")) {
                adicionarColunas(colunas, "", repeticoes(reader));
            }
        }

        return colunas;
    }

    private String lerCelula(
            XMLStreamReader reader,
            int numeroLinha,
            int numeroColuna
    ) throws XMLStreamException {
        StringBuilder texto = new StringBuilder();
        int profundidade = 1;
        int dentroDeParagrafo = 0;

        while (reader.hasNext() && profundidade > 0) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                profundidade++;
                if (elemento(reader, TEXT_NS, "p")) {
                    if (!texto.isEmpty()) {
                        adicionarTextoCelula(
                                texto,
                                " ",
                                numeroLinha,
                                numeroColuna
                        );
                    }
                    dentroDeParagrafo++;
                }
            } else if (event == XMLStreamConstants.CHARACTERS && dentroDeParagrafo > 0) {
                adicionarTextoCelula(
                        texto,
                        reader.getText(),
                        numeroLinha,
                        numeroColuna
                );
            } else if (event == XMLStreamConstants.DTD
                    || event == XMLStreamConstants.ENTITY_REFERENCE) {
                throw new ImportacaoOdsException(
                        "O conteúdo XML do ODS contém DTD ou entidade, o que não é permitido."
                );
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (elemento(reader, TEXT_NS, "p")) {
                    dentroDeParagrafo--;
                }
                profundidade--;
            }
        }

        return normalizarTexto(texto.toString());
    }

    private void adicionarTextoCelula(
            StringBuilder destino,
            String trecho,
            int numeroLinha,
            int numeroColuna
    ) {
        int caracteresAtuais = destino.codePointCount(0, destino.length());
        int caracteresNovos = trecho.codePointCount(0, trecho.length());
        if (caracteresAtuais + caracteresNovos > MAX_CARACTERES_CELULA) {
            throw new ImportacaoOdsException(
                    "O ODS contém célula com mais de 75 caracteres na linha "
                            + numeroLinha
                            + ", coluna "
                            + numeroColuna
                            + "."
            );
        }
        destino.append(trecho);
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

    private String normalizarCabecalho(String valor) {
        return semAcentos(normalizarTexto(valor))
                .toLowerCase(Locale.ROOT)
                .replace("%", " ")
                .replace("³", "3")
                .replaceAll("[^a-z0-9]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }

    private String normalizarCodigoProduto(String codigo) {
        String normalizado = codigo.replace(".", "").replaceAll("\\s+", "");
        return CODIGO_PRODUTO.matcher(codigo).matches()
                && normalizado.matches("\\d+")
                ? normalizado
                : null;
    }

    private BigDecimal decimalOuZero(String valor, int numeroLinha, String campo) {
        BigDecimal decimal = decimalOpcional(valor, numeroLinha, campo);
        return decimal == null ? BigDecimal.ZERO : decimal;
    }

    private BigDecimal decimalOpcional(String valor, int numeroLinha, String campo) {
        String normalizado = valor
                .replace("\u00A0", "")
                .replace(" ", "")
                .replace(".", "")
                .replace(",", ".");
        if (normalizado.isBlank() || "-".equals(normalizado)) {
            return null;
        }
        try {
            BigDecimal decimal = new BigDecimal(normalizado);
            if (decimal.signum() < 0) {
                throw erroLinha(numeroLinha, campo + " não pode ser negativo: " + valor);
            }
            return decimal;
        } catch (NumberFormatException e) {
            throw erroLinha(numeroLinha, campo + " inválido: " + valor);
        }
    }

    private LocalDate dataOpcional(String valor, int numeroLinha) {
        String normalizado = normalizarTexto(valor);
        if (normalizado.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(normalizado, DATA_BRASILEIRA);
        } catch (DateTimeParseException e) {
            throw erroLinha(numeroLinha, "Data cadastro inválida: " + valor);
        }
    }

    private Boolean booleanoOpcional(
            String valor,
            int numeroLinha,
            String campo
    ) {
        String normalizado = semAcentos(normalizarTexto(valor)).toLowerCase(Locale.ROOT);
        if (normalizado.isBlank()) {
            return null;
        }
        if ("sim".equals(normalizado)) {
            return true;
        }
        if ("nao".equals(normalizado)) {
            return false;
        }
        throw erroLinha(numeroLinha, campo + " inválido: " + valor);
    }

    private String textoOpcional(String valor) {
        String normalizado = normalizarTexto(valor);
        return normalizado.isBlank() ? null : normalizado;
    }

    private String campo(
            List<String> colunas,
            Map<String, Integer> cabecalhos,
            String nomeCabecalho
    ) {
        Integer indice = cabecalhos.get(nomeCabecalho);
        return indice != null && indice < colunas.size() ? colunas.get(indice) : "";
    }

    private boolean linhaVazia(List<String> colunas) {
        return colunas.stream().allMatch(String::isBlank);
    }

    private String normalizarTexto(String valor) {
        return valor == null ? "" : valor.trim().replaceAll("\\s+", " ");
    }

    private String semAcentos(String valor) {
        return Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
    }

    private boolean elemento(XMLStreamReader reader, String namespace, String nomeLocal) {
        return namespace.equals(reader.getNamespaceURI())
                && nomeLocal.equals(reader.getLocalName());
    }

    void configurarXmlSeguro(XMLInputFactory factory) {
        exigirPropriedadeXmlDesativada(factory, XMLInputFactory.SUPPORT_DTD);
        exigirPropriedadeXmlDesativada(
                factory,
                "javax.xml.stream.isSupportingExternalEntities"
        );
        exigirPropriedadeXmlDesativada(
                factory,
                XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES
        );
        factory.setXMLResolver((publicId, systemId, baseUri, namespace) -> {
            throw new XMLStreamException("Acesso a recurso XML externo bloqueado.");
        });
    }

    private void exigirPropriedadeXmlDesativada(
            XMLInputFactory factory,
            String propriedade
    ) {
        try {
            factory.setProperty(propriedade, Boolean.FALSE);
            if (!Boolean.FALSE.equals(factory.getProperty(propriedade))) {
                throw new ImportacaoOdsException(
                        "O parser XML não confirmou as proteções obrigatórias."
                );
            }
        } catch (IllegalArgumentException e) {
            throw new ImportacaoOdsException(
                    "O parser XML não oferece as proteções obrigatórias.",
                    e
            );
        }
    }

    private static final class InputStreamLimitado extends FilterInputStream {

        private final long limite;
        private long lidos;

        private InputStreamLimitado(InputStream input, long limite) {
            super(input);
            this.limite = limite;
        }

        @Override
        public int read() throws IOException {
            int valor = super.read();
            if (valor != -1) {
                contabilizar(1);
            }
            return valor;
        }

        @Override
        public int read(byte[] buffer, int offset, int tamanho) throws IOException {
            int quantidade = super.read(buffer, offset, tamanho);
            if (quantidade > 0) {
                contabilizar(quantidade);
            }
            return quantidade;
        }

        private void contabilizar(long quantidade) throws IOException {
            lidos += quantidade;
            if (lidos > limite) {
                throw new IOException("Limite de descompactação excedido.");
            }
        }
    }

    private ImportacaoOdsException erroLinha(int numeroLinha, String detalhe) {
        return new ImportacaoOdsException("Linha " + numeroLinha + ": " + detalhe);
    }
}
