package com.ecommerceproject.dubaimagazinesalvador.services.importacao;

import java.io.IOException;
import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.ecommerceproject.dubaimagazinesalvador.domain.importacao.CategoriaImportacaoDTO;
import com.ecommerceproject.dubaimagazinesalvador.domain.importacao.ImportacaoProdutosResponseDTO;
import com.ecommerceproject.dubaimagazinesalvador.domain.importacao.RelacaoProdutosOdsDTO;
import com.ecommerceproject.dubaimagazinesalvador.domain.produto.ProdutoImportacaoDTO;

@Service
public class ImportacaoProdutosService {

    private static final long TAMANHO_MAXIMO_ARQUIVO = 20L * 1024L * 1024L;
    private static final String MIME_ODS =
            "application/vnd.oasis.opendocument.spreadsheet";
    private static final int TAMANHO_LOTE = 1_000;
    private static final String CODIGO_ATIVO_IMOBILIZADO = "089";
    private static final String CODIGO_AJUSTES_DE_GRUPOS = "123";
    private static final String CODIGO_IMPLANTACAO = "999";

    private static final String UPSERT_CATEGORIA = """
            INSERT INTO categorias (
                codigo, nome, nivel, caminho, categoria_pai_codigo, exibir_no_site
            ) VALUES (?, ?, ?, ?, ?, TRUE)
            ON DUPLICATE KEY UPDATE
                nome = VALUES(nome),
                nivel = VALUES(nivel),
                caminho = VALUES(caminho),
                categoria_pai_codigo = VALUES(categoria_pai_codigo)
            """;

    private static final String UPSERT_PRODUTO = """
            INSERT INTO produtos (
                codigo_santri,
                nome,
                nome_exibido_site,
                ncm,
                nome_compra,
                fabricante,
                marca,
                ativo_santri,
                unidade_venda,
                unidade_compra,
                data_cadastro,
                codigo_original,
                codigo_barras,
                bloqueado_para_compras,
                estoque,
                preco_sem_ipi,
                percentual_ipi_entrada,
                peso_unidade,
                altura_unidade,
                largura_unidade,
                comprimento_unidade,
                volume_unidade_m3,
                volume_litros,
                peso_caixa,
                altura_caixa,
                largura_caixa,
                comprimento_caixa,
                origem,
                industrializado,
                insumo,
                percentual_maximo_aproveitamento_ipi,
                numero_fci,
                categoria_codigo,
                exibir_no_site,
                destaque_na_home,
                disponivel_ultima_importacao,
                ultima_importacao_em
            ) VALUES (
                ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                FALSE, FALSE, TRUE, ?
            )
            ON DUPLICATE KEY UPDATE
                nome_exibido_site = CASE
                    WHEN nome_exibido_site IS NULL
                         OR TRIM(nome_exibido_site) = ''
                         OR nome_exibido_site = nome
                    THEN VALUES(nome)
                    ELSE nome_exibido_site
                END,
                nome = VALUES(nome),
                ncm = VALUES(ncm),
                nome_compra = VALUES(nome_compra),
                fabricante = VALUES(fabricante),
                marca = VALUES(marca),
                ativo_santri = VALUES(ativo_santri),
                unidade_venda = VALUES(unidade_venda),
                unidade_compra = VALUES(unidade_compra),
                data_cadastro = VALUES(data_cadastro),
                codigo_original = VALUES(codigo_original),
                codigo_barras = VALUES(codigo_barras),
                bloqueado_para_compras = VALUES(bloqueado_para_compras),
                estoque = VALUES(estoque),
                preco_sem_ipi = VALUES(preco_sem_ipi),
                percentual_ipi_entrada = VALUES(percentual_ipi_entrada),
                peso_unidade = VALUES(peso_unidade),
                altura_unidade = VALUES(altura_unidade),
                largura_unidade = VALUES(largura_unidade),
                comprimento_unidade = VALUES(comprimento_unidade),
                volume_unidade_m3 = VALUES(volume_unidade_m3),
                volume_litros = VALUES(volume_litros),
                peso_caixa = VALUES(peso_caixa),
                altura_caixa = VALUES(altura_caixa),
                largura_caixa = VALUES(largura_caixa),
                comprimento_caixa = VALUES(comprimento_caixa),
                origem = VALUES(origem),
                industrializado = VALUES(industrializado),
                insumo = VALUES(insumo),
                percentual_maximo_aproveitamento_ipi =
                    VALUES(percentual_maximo_aproveitamento_ipi),
                numero_fci = VALUES(numero_fci),
                exibir_no_site = CASE
                    WHEN VALUES(preco_sem_ipi) <= 0 THEN FALSE
                    WHEN categoria_codigo <> VALUES(categoria_codigo)
                         AND (
                             VALUES(categoria_codigo) = '123'
                             OR VALUES(categoria_codigo) LIKE '123.%'
                         )
                    THEN FALSE
                    ELSE exibir_no_site
                END,
                destaque_na_home = CASE
                    WHEN VALUES(preco_sem_ipi) <= 0 THEN FALSE
                    WHEN categoria_codigo <> VALUES(categoria_codigo)
                         AND (
                             VALUES(categoria_codigo) = '123'
                             OR VALUES(categoria_codigo) LIKE '123.%'
                         )
                    THEN FALSE
                    ELSE destaque_na_home
                END,
                categoria_codigo = VALUES(categoria_codigo),
                disponivel_ultima_importacao = TRUE,
                ultima_importacao_em = VALUES(ultima_importacao_em)
            """;

    private final LeitorRelacaoProdutosOds leitorRelacaoProdutosOds;
    private final JdbcTemplate jdbcTemplate;

    public ImportacaoProdutosService(
            LeitorRelacaoProdutosOds leitorRelacaoProdutosOds,
            JdbcTemplate jdbcTemplate
    ) {
        this.leitorRelacaoProdutosOds = leitorRelacaoProdutosOds;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public ImportacaoProdutosResponseDTO importar(MultipartFile arquivo) {
        validarArquivo(arquivo);
        long inicio = System.nanoTime();
        LocalDateTime importadoEm = LocalDateTime.now();
        RelacaoProdutosOdsDTO relacaoLida = lerArquivo(arquivo);
        RelacaoProdutosOdsDTO relacao = aplicarRegrasDeCatalogo(relacaoLida);

        Set<String> categoriasExistentes = new HashSet<>(
                jdbcTemplate.queryForList("SELECT codigo FROM categorias", String.class)
        );
        Set<String> produtosExistentes = new HashSet<>(
                jdbcTemplate.queryForList("SELECT codigo_santri FROM produtos", String.class)
        );

        removerAtivosImobilizados(relacaoLida.produtos());
        salvarCategorias(relacao.categorias());
        salvarProdutos(relacao.produtos(), importadoEm);
        marcarProdutosAusentesComoIndisponiveis(importadoEm);
        manterCategoriasInternasOcultas();

        int categoriasAtualizadas = contarExistentesCategorias(
                relacao.categorias(),
                categoriasExistentes
        );
        int produtosAtualizados = contarExistentesProdutos(
                relacao.produtos(),
                produtosExistentes
        );
        String nomeArquivo = StringUtils.cleanPath(
                arquivo.getOriginalFilename() == null
                        ? "relacao-produtos.ods"
                        : arquivo.getOriginalFilename()
        );

        return new ImportacaoProdutosResponseDTO(
                nomeArquivo,
                relacao.categorias().size(),
                relacao.categorias().size() - categoriasAtualizadas,
                categoriasAtualizadas,
                relacao.produtos().size(),
                relacao.produtos().size() - produtosAtualizados,
                produtosAtualizados,
                relacao.linhasIgnoradas(),
                importadoEm,
                Duration.ofNanos(System.nanoTime() - inicio).toMillis()
        );
    }

    private RelacaoProdutosOdsDTO aplicarRegrasDeCatalogo(
            RelacaoProdutosOdsDTO relacao
    ) {
        List<ProdutoImportacaoDTO> produtos = relacao.produtos().stream()
                .filter(produto -> !pertenceAArvore(
                        produto.categoriaCodigo(),
                        CODIGO_ATIVO_IMOBILIZADO
                ))
                .toList();

        Set<String> categoriasNecessarias = new HashSet<>();
        produtos.forEach(produto -> adicionarCategoriaEAncestrais(
                produto.categoriaCodigo(),
                categoriasNecessarias
        ));

        List<CategoriaImportacaoDTO> categorias = relacao.categorias().stream()
                .filter(categoria -> !pertenceAArvore(
                        categoria.codigo(),
                        CODIGO_ATIVO_IMOBILIZADO
                ))
                .filter(categoria -> categoriasNecessarias.contains(categoria.codigo()))
                .toList();

        return new RelacaoProdutosOdsDTO(
                categorias,
                produtos,
                relacao.linhasIgnoradas()
        );
    }

    private void adicionarCategoriaEAncestrais(String codigo, Set<String> destino) {
        String atual = codigo;
        while (atual != null && !atual.isBlank()) {
            destino.add(atual);
            int ultimoPonto = atual.lastIndexOf('.');
            atual = ultimoPonto < 0 ? null : atual.substring(0, ultimoPonto);
        }
    }

    private void removerAtivosImobilizados(List<ProdutoImportacaoDTO> produtosLidos) {
        List<ProdutoImportacaoDTO> ativosLidos = produtosLidos.stream()
                .filter(produto -> pertenceAArvore(
                        produto.categoriaCodigo(),
                        CODIGO_ATIVO_IMOBILIZADO
                ))
                .toList();

        if (!ativosLidos.isEmpty()) {
            jdbcTemplate.batchUpdate(
                    "DELETE FROM produtos WHERE codigo_santri = ?",
                    ativosLidos,
                    TAMANHO_LOTE,
                    (statement, produto) -> statement.setString(1, produto.codigoSantri())
            );
        }

        jdbcTemplate.update("""
                DELETE FROM vitrines_home
                WHERE categoria_codigo = ?
                   OR categoria_codigo LIKE CONCAT(?, '.%')
                """, CODIGO_ATIVO_IMOBILIZADO, CODIGO_ATIVO_IMOBILIZADO);
        jdbcTemplate.update("""
                DELETE FROM produtos
                WHERE categoria_codigo = ?
                   OR categoria_codigo LIKE CONCAT(?, '.%')
                """, CODIGO_ATIVO_IMOBILIZADO, CODIGO_ATIVO_IMOBILIZADO);

        for (int nivel = 4; nivel >= 1; nivel--) {
            jdbcTemplate.update("""
                    DELETE FROM categorias
                    WHERE nivel = ?
                      AND (
                          codigo = ?
                          OR codigo LIKE CONCAT(?, '.%')
                      )
                    """, nivel, CODIGO_ATIVO_IMOBILIZADO, CODIGO_ATIVO_IMOBILIZADO);
        }
    }

    private void marcarProdutosAusentesComoIndisponiveis(LocalDateTime importadoEm) {
        jdbcTemplate.update("""
                UPDATE produtos
                SET disponivel_ultima_importacao = FALSE,
                    exibir_no_site = FALSE,
                    destaque_na_home = FALSE
                WHERE ultima_importacao_em IS NULL
                   OR ultima_importacao_em < ?
                """, importadoEm);
    }

    private void manterCategoriasInternasOcultas() {
        jdbcTemplate.update("""
                UPDATE categorias
                SET exibir_no_site = FALSE
                WHERE codigo = ?
                   OR codigo LIKE CONCAT(?, '.%')
                   OR codigo = ?
                   OR codigo LIKE CONCAT(?, '.%')
                """,
                CODIGO_AJUSTES_DE_GRUPOS,
                CODIGO_AJUSTES_DE_GRUPOS,
                CODIGO_IMPLANTACAO,
                CODIGO_IMPLANTACAO
        );
    }

    private boolean pertenceAArvore(String codigo, String codigoRaiz) {
        return codigoRaiz.equals(codigo) || codigo.startsWith(codigoRaiz + ".");
    }

    private RelacaoProdutosOdsDTO lerArquivo(MultipartFile arquivo) {
        try (InputStream input = arquivo.getInputStream()) {
            return leitorRelacaoProdutosOds.ler(input);
        } catch (IOException e) {
            throw new ImportacaoOdsException(
                    "Não foi possível ler o arquivo enviado.",
                    e
            );
        }
    }

    private void salvarCategorias(List<CategoriaImportacaoDTO> categorias) {
        jdbcTemplate.batchUpdate(
                UPSERT_CATEGORIA,
                categorias,
                TAMANHO_LOTE,
                this::preencherCategoria
        );
    }

    private void salvarProdutos(List<ProdutoImportacaoDTO> produtos, LocalDateTime importadoEm) {
        jdbcTemplate.batchUpdate(
                UPSERT_PRODUTO,
                produtos,
                TAMANHO_LOTE,
                (statement, produto) -> preencherProduto(statement, produto, importadoEm)
        );
    }

    private void preencherCategoria(
            PreparedStatement statement,
            CategoriaImportacaoDTO categoria
    ) throws SQLException {
        statement.setString(1, categoria.codigo());
        statement.setString(2, categoria.nome());
        statement.setInt(3, categoria.nivel());
        statement.setString(4, categoria.caminho());
        if (categoria.categoriaPaiCodigo() == null) {
            statement.setNull(5, Types.VARCHAR);
        } else {
            statement.setString(5, categoria.categoriaPaiCodigo());
        }
    }

    private void preencherProduto(
            PreparedStatement statement,
            ProdutoImportacaoDTO produto,
            LocalDateTime importadoEm
    ) throws SQLException {
        statement.setString(1, produto.codigoSantri());
        statement.setString(2, produto.nome());
        statement.setString(3, produto.nome());
        statement.setString(4, produto.ncm());
        statement.setString(5, produto.nomeCompra());
        statement.setString(6, produto.fabricante());
        statement.setString(7, produto.marca());
        statement.setBoolean(8, produto.ativoSantri());
        statement.setString(9, produto.unidadeVenda());
        statement.setString(10, produto.unidadeCompra());
        statement.setObject(11, produto.dataCadastro());
        statement.setString(12, produto.codigoOriginal());
        statement.setString(13, produto.codigoBarras());
        statement.setBoolean(14, produto.bloqueadoParaCompras());
        statement.setBigDecimal(15, produto.estoque());
        statement.setBigDecimal(16, produto.precoSemIpi());
        statement.setBigDecimal(17, produto.percentualIpiEntrada());
        statement.setBigDecimal(18, produto.pesoUnidade());
        statement.setBigDecimal(19, produto.alturaUnidade());
        statement.setBigDecimal(20, produto.larguraUnidade());
        statement.setBigDecimal(21, produto.comprimentoUnidade());
        statement.setBigDecimal(22, produto.volumeUnidadeM3());
        statement.setBigDecimal(23, produto.volumeLitros());
        statement.setBigDecimal(24, produto.pesoCaixa());
        statement.setBigDecimal(25, produto.alturaCaixa());
        statement.setBigDecimal(26, produto.larguraCaixa());
        statement.setBigDecimal(27, produto.comprimentoCaixa());
        statement.setString(28, produto.origem());
        setBooleanOpcional(statement, 29, produto.industrializado());
        setBooleanOpcional(statement, 30, produto.insumo());
        statement.setBigDecimal(31, produto.percentualMaximoAproveitamentoIpi());
        statement.setString(32, produto.numeroFci());
        statement.setString(33, produto.categoriaCodigo());
        statement.setObject(34, importadoEm);
    }

    private void setBooleanOpcional(
            PreparedStatement statement,
            int indice,
            Boolean valor
    ) throws SQLException {
        if (valor == null) {
            statement.setNull(indice, Types.BOOLEAN);
        } else {
            statement.setBoolean(indice, valor);
        }
    }

    private int contarExistentesCategorias(
            List<CategoriaImportacaoDTO> categorias,
            Set<String> existentes
    ) {
        return (int) categorias.stream()
                .map(CategoriaImportacaoDTO::codigo)
                .filter(existentes::contains)
                .count();
    }

    private int contarExistentesProdutos(
            List<ProdutoImportacaoDTO> produtos,
            Set<String> existentes
    ) {
        return (int) produtos.stream()
                .map(ProdutoImportacaoDTO::codigoSantri)
                .filter(existentes::contains)
                .count();
    }

    private void validarArquivo(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new ImportacaoOdsException(
                    "Selecione o arquivo ODS da relação analítica de produtos."
            );
        }
        String nome = arquivo.getOriginalFilename();
        if (nome == null
                || nome.length() > 255
                || nome.indexOf('\0') >= 0
                || !nome.toLowerCase(Locale.ROOT).endsWith(".ods")) {
            throw new ImportacaoOdsException("O arquivo deve possuir a extensão .ods.");
        }
        if (arquivo.getSize() > TAMANHO_MAXIMO_ARQUIVO) {
            throw new ImportacaoOdsException("O arquivo excede o limite de 20 MB.");
        }
        String contentType = arquivo.getContentType();
        if (contentType != null
                && !contentType.isBlank()
                && !MIME_ODS.equalsIgnoreCase(contentType)
                && !"application/octet-stream".equalsIgnoreCase(contentType)) {
            throw new ImportacaoOdsException("O tipo de arquivo enviado não é ODS.");
        }
        validarAssinaturaZip(arquivo);
    }

    private void validarAssinaturaZip(MultipartFile arquivo) {
        try (InputStream input = arquivo.getInputStream()) {
            byte[] assinatura = input.readNBytes(4);
            if (assinatura.length != 4
                    || assinatura[0] != 'P'
                    || assinatura[1] != 'K'
                    || assinatura[2] != 3
                    || assinatura[3] != 4) {
                throw new ImportacaoOdsException(
                        "O conteúdo do arquivo não corresponde a um ODS válido."
                );
            }
        } catch (IOException e) {
            throw new ImportacaoOdsException("Não foi possível validar o arquivo enviado.", e);
        }
    }
}
