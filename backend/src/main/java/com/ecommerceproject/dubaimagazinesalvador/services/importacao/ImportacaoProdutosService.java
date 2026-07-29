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
import java.util.Set;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.ecommerceproject.dubaimagazinesalvador.domain.importacao.CategoriaImportacaoDTO;
import com.ecommerceproject.dubaimagazinesalvador.domain.importacao.ImportacaoProdutosResponseDTO;
import com.ecommerceproject.dubaimagazinesalvador.domain.importacao.InventarioOdsDTO;
import com.ecommerceproject.dubaimagazinesalvador.domain.produto.ProdutoImportacaoDTO;

@Service
public class ImportacaoProdutosService {

    private static final long TAMANHO_MAXIMO_ARQUIVO = 20L * 1024L * 1024L;
    private static final int TAMANHO_LOTE = 1_000;
    private static final String CODIGO_ATIVO_IMOBILIZADO = "089";
    private static final String CODIGO_AJUSTES_DE_GRUPOS = "123";

    private static final String UPSERT_CATEGORIA = """
            INSERT INTO categorias (
                codigo, nome, nivel, caminho, categoria_pai_codigo, exibir_no_site
            ) VALUES (?, ?, ?, ?, ?, FALSE)
            ON DUPLICATE KEY UPDATE
                nome = VALUES(nome),
                nivel = VALUES(nivel),
                caminho = VALUES(caminho),
                categoria_pai_codigo = VALUES(categoria_pai_codigo)
            """;

    private static final String UPSERT_PRODUTO = """
            INSERT INTO produtos (
                codigo_santri, descricao, nome_exibido_site, ncm, unidade, marca, codigo_original,
                quantidade, preco_venda, preco_venda_iva, categoria_codigo,
                exibir_no_site, destaque_na_home, ultima_importacao_em
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, FALSE, FALSE, ?)
            ON DUPLICATE KEY UPDATE
                nome_exibido_site = CASE
                    WHEN nome_exibido_site IS NULL OR nome_exibido_site = descricao
                    THEN VALUES(descricao)
                    ELSE nome_exibido_site
                END,
                descricao = VALUES(descricao),
                ncm = VALUES(ncm),
                unidade = VALUES(unidade),
                marca = VALUES(marca),
                codigo_original = VALUES(codigo_original),
                quantidade = VALUES(quantidade),
                preco_venda = VALUES(preco_venda),
                preco_venda_iva = VALUES(preco_venda_iva),
                exibir_no_site = CASE
                    WHEN categoria_codigo <> VALUES(categoria_codigo)
                         AND (
                             VALUES(categoria_codigo) = '123'
                             OR VALUES(categoria_codigo) LIKE '123.%'
                         )
                    THEN FALSE
                    ELSE exibir_no_site
                END,
                destaque_na_home = CASE
                    WHEN categoria_codigo <> VALUES(categoria_codigo)
                         AND (
                             VALUES(categoria_codigo) = '123'
                             OR VALUES(categoria_codigo) LIKE '123.%'
                         )
                    THEN FALSE
                    ELSE destaque_na_home
                END,
                categoria_codigo = VALUES(categoria_codigo),
                ultima_importacao_em = VALUES(ultima_importacao_em)
            """;

    private final LeitorInventarioOds leitorInventarioOds;
    private final JdbcTemplate jdbcTemplate;

    public ImportacaoProdutosService(
            LeitorInventarioOds leitorInventarioOds,
            JdbcTemplate jdbcTemplate
    ) {
        this.leitorInventarioOds = leitorInventarioOds;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public ImportacaoProdutosResponseDTO importar(MultipartFile arquivo) {
        validarArquivo(arquivo);
        long inicio = System.nanoTime();
        LocalDateTime importadoEm = LocalDateTime.now();
        InventarioOdsDTO inventarioLido = lerArquivo(arquivo);
        InventarioOdsDTO inventario = aplicarRegrasDeCatalogo(inventarioLido);

        Set<String> categoriasExistentes = new HashSet<>(
                jdbcTemplate.queryForList("SELECT codigo FROM categorias", String.class)
        );
        Set<String> produtosExistentes = new HashSet<>(
                jdbcTemplate.queryForList("SELECT codigo_santri FROM produtos", String.class)
        );

        removerAtivosImobilizados(inventarioLido.produtos());
        salvarCategorias(inventario.categorias());
        salvarProdutos(inventario.produtos(), importadoEm);
        manterAjustesDeGruposOculto();

        int categoriasAtualizadas = contarExistentesCategorias(
                inventario.categorias(),
                categoriasExistentes
        );
        int produtosAtualizados = contarExistentesProdutos(
                inventario.produtos(),
                produtosExistentes
        );
        String nomeArquivo = StringUtils.cleanPath(
                arquivo.getOriginalFilename() == null ? "inventario.ods" : arquivo.getOriginalFilename()
        );

        return new ImportacaoProdutosResponseDTO(
                nomeArquivo,
                inventario.categorias().size(),
                inventario.categorias().size() - categoriasAtualizadas,
                categoriasAtualizadas,
                inventario.produtos().size(),
                inventario.produtos().size() - produtosAtualizados,
                produtosAtualizados,
                inventario.linhasIgnoradas(),
                importadoEm,
                Duration.ofNanos(System.nanoTime() - inicio).toMillis()
        );
    }

    private InventarioOdsDTO aplicarRegrasDeCatalogo(InventarioOdsDTO inventario) {
        List<CategoriaImportacaoDTO> categorias = inventario.categorias().stream()
                .filter(categoria -> !pertenceAArvore(
                        categoria.codigo(),
                        CODIGO_ATIVO_IMOBILIZADO
                ))
                .toList();
        List<ProdutoImportacaoDTO> produtos = inventario.produtos().stream()
                .filter(produto -> !pertenceAArvore(
                        produto.categoriaCodigo(),
                        CODIGO_ATIVO_IMOBILIZADO
                ))
                .toList();

        return new InventarioOdsDTO(categorias, produtos, inventario.linhasIgnoradas());
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

    private void manterAjustesDeGruposOculto() {
        jdbcTemplate.update("""
                UPDATE categorias
                SET exibir_no_site = FALSE
                WHERE codigo = ?
                   OR codigo LIKE CONCAT(?, '.%')
                """, CODIGO_AJUSTES_DE_GRUPOS, CODIGO_AJUSTES_DE_GRUPOS);
    }

    private boolean pertenceAArvore(String codigo, String codigoRaiz) {
        return codigoRaiz.equals(codigo) || codigo.startsWith(codigoRaiz + ".");
    }

    private InventarioOdsDTO lerArquivo(MultipartFile arquivo) {
        try (InputStream input = arquivo.getInputStream()) {
            return leitorInventarioOds.ler(input);
        } catch (IOException e) {
            throw new ImportacaoOdsException("Não foi possível ler o arquivo enviado.", e);
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
        statement.setString(2, produto.descricao());
        statement.setString(3, produto.descricao());
        statement.setString(4, produto.ncm());
        statement.setString(5, produto.unidade());
        statement.setString(6, produto.marca());
        statement.setString(7, produto.codigoOriginal());
        statement.setBigDecimal(8, produto.quantidade());
        statement.setBigDecimal(9, produto.precoVenda());
        statement.setBigDecimal(10, produto.precoVendaIva());
        statement.setString(11, produto.categoriaCodigo());
        statement.setObject(12, importadoEm);
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
            throw new ImportacaoOdsException("Selecione um arquivo ODS para importar.");
        }
        String nome = arquivo.getOriginalFilename();
        if (nome == null || !nome.toLowerCase().endsWith(".ods")) {
            throw new ImportacaoOdsException("O arquivo deve possuir a extensão .ods.");
        }
        if (arquivo.getSize() > TAMANHO_MAXIMO_ARQUIVO) {
            throw new ImportacaoOdsException("O arquivo excede o limite de 20 MB.");
        }
    }
}
