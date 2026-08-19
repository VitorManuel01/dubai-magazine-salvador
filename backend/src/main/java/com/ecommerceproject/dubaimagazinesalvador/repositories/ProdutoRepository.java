package com.ecommerceproject.dubaimagazinesalvador.repositories;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ecommerceproject.dubaimagazinesalvador.domain.produto.Produto;

@Repository
public interface ProdutoRepository extends JpaRepository<Produto, String> {

    long countByDestaqueNaHomeTrue();

    @EntityGraph(attributePaths = {"categoria", "imagens"})
    Optional<Produto> findByIdPublico(String idPublico);

    @EntityGraph(attributePaths = {"categoria", "imagens"})
    @Query("""
            SELECT produto
            FROM Produto produto
            JOIN produto.categoria categoria
            WHERE produto.idPublico = :idPublico
              AND produto.exibirNoSite = true
              AND produto.disponivelUltimaImportacao = true
              AND categoria.exibirNoSite = true
              AND categoria.codigo <> '008'
              AND categoria.codigo NOT LIKE '008.%'
              AND UPPER(categoria.caminho) NOT LIKE 'USO E CONSUMO%'
            """)
    Optional<Produto> findDetalhePublicoByIdPublico(@Param("idPublico") String idPublico);

    @Query(
            value = """
            SELECT produto
            FROM Produto produto
            JOIN FETCH produto.categoria categoria
            WHERE (
                    :incluirOcultos = true
                    OR (
                        produto.exibirNoSite = true
                        AND produto.disponivelUltimaImportacao = true
                    )
              )
              AND (
                    :incluirOcultos = true
                    OR (
                        categoria.exibirNoSite = true
                        AND categoria.codigo <> '008'
                        AND categoria.codigo NOT LIKE '008.%'
                        AND UPPER(categoria.caminho) NOT LIKE 'USO E CONSUMO%'
                    )
              )
              AND (:somenteDestaques = false OR produto.destaqueNaHome = true)
              AND (
                    :precoMinimo IS NULL
                    OR ROUND(
                        produto.precoSemIpi
                        * (1 + COALESCE(produto.percentualIpiEntrada, 0) / 100),
                        2
                    ) >= :precoMinimo
              )
              AND (
                    :precoMaximo IS NULL
                    OR ROUND(
                        produto.precoSemIpi
                        * (1 + COALESCE(produto.percentualIpiEntrada, 0) / 100),
                        2
                    ) <= :precoMaximo
              )
              AND (
                    :categoriaCodigo IS NULL
                    OR categoria.codigo = :categoriaCodigo
                    OR categoria.codigo LIKE CONCAT(:categoriaCodigo, '.%')
              )
              AND (
                    :busca IS NULL
                    OR LOWER(produto.nomeExibidoSite) LIKE CONCAT('%', LOWER(:busca), '%')
                    OR LOWER(produto.marca) LIKE CONCAT('%', LOWER(:busca), '%')
                    OR (
                        :incluirOcultos = true
                        AND (
                            LOWER(produto.nome) LIKE CONCAT('%', LOWER(:busca), '%')
                            OR LOWER(produto.codigoSantri) LIKE CONCAT('%', LOWER(:busca), '%')
                            OR LOWER(produto.codigoOriginal) LIKE CONCAT('%', LOWER(:busca), '%')
                            OR LOWER(produto.codigoBarras) LIKE CONCAT('%', LOWER(:busca), '%')
                            OR LOWER(produto.fabricante) LIKE CONCAT('%', LOWER(:busca), '%')
                        )
                    )
              )
            ORDER BY
                CASE
                    WHEN :incluirOcultos = true
                         AND produto.exibirNoSite = true
                         AND produto.disponivelUltimaImportacao = true
                    THEN 0
                    WHEN :incluirOcultos = true THEN 1
                    ELSE 0
                END ASC,
                produto.nomeExibidoSite ASC,
                produto.codigoSantri ASC
            """,
            countQuery = """
            SELECT COUNT(produto)
            FROM Produto produto
            JOIN produto.categoria categoria
            WHERE (
                    :incluirOcultos = true
                    OR (
                        produto.exibirNoSite = true
                        AND produto.disponivelUltimaImportacao = true
                    )
              )
              AND (
                    :incluirOcultos = true
                    OR (
                        categoria.exibirNoSite = true
                        AND categoria.codigo <> '008'
                        AND categoria.codigo NOT LIKE '008.%'
                        AND UPPER(categoria.caminho) NOT LIKE 'USO E CONSUMO%'
                    )
              )
              AND (:somenteDestaques = false OR produto.destaqueNaHome = true)
              AND (
                    :precoMinimo IS NULL
                    OR ROUND(
                        produto.precoSemIpi
                        * (1 + COALESCE(produto.percentualIpiEntrada, 0) / 100),
                        2
                    ) >= :precoMinimo
              )
              AND (
                    :precoMaximo IS NULL
                    OR ROUND(
                        produto.precoSemIpi
                        * (1 + COALESCE(produto.percentualIpiEntrada, 0) / 100),
                        2
                    ) <= :precoMaximo
              )
              AND (
                    :categoriaCodigo IS NULL
                    OR categoria.codigo = :categoriaCodigo
                    OR categoria.codigo LIKE CONCAT(:categoriaCodigo, '.%')
              )
              AND (
                    :busca IS NULL
                    OR LOWER(produto.nomeExibidoSite) LIKE CONCAT('%', LOWER(:busca), '%')
                    OR LOWER(produto.marca) LIKE CONCAT('%', LOWER(:busca), '%')
                    OR (
                        :incluirOcultos = true
                        AND (
                            LOWER(produto.nome) LIKE CONCAT('%', LOWER(:busca), '%')
                            OR LOWER(produto.codigoSantri) LIKE CONCAT('%', LOWER(:busca), '%')
                            OR LOWER(produto.codigoOriginal) LIKE CONCAT('%', LOWER(:busca), '%')
                            OR LOWER(produto.codigoBarras) LIKE CONCAT('%', LOWER(:busca), '%')
                            OR LOWER(produto.fabricante) LIKE CONCAT('%', LOWER(:busca), '%')
                        )
                    )
              )
            """
    )
    Page<Produto> findCatalogoPorCategoria(
            @Param("categoriaCodigo") String categoriaCodigo,
            @Param("busca") String busca,
            @Param("incluirOcultos") boolean incluirOcultos,
            @Param("somenteDestaques") boolean somenteDestaques,
            @Param("precoMinimo") BigDecimal precoMinimo,
            @Param("precoMaximo") BigDecimal precoMaximo,
            Pageable pageable
    );

    default Page<Produto> findCatalogoPorCategoria(
            String categoriaCodigo,
            String busca,
            boolean incluirOcultos,
            boolean somenteDestaques,
            Pageable pageable
    ) {
        return findCatalogoPorCategoria(
                categoriaCodigo,
                busca,
                incluirOcultos,
                somenteDestaques,
                null,
                null,
                pageable
        );
    }

    @Query(
            value = """
            SELECT produto
            FROM Produto produto
            JOIN FETCH produto.categoria categoria
            WHERE produto.exibirNoSite = true
              AND produto.disponivelUltimaImportacao = true
              AND (:somenteDestaques = false OR produto.destaqueNaHome = true)
              AND (
                    :precoMinimo IS NULL
                    OR ROUND(
                        produto.precoSemIpi
                        * (1 + COALESCE(produto.percentualIpiEntrada, 0) / 100),
                        2
                    ) >= :precoMinimo
              )
              AND (
                    :precoMaximo IS NULL
                    OR ROUND(
                        produto.precoSemIpi
                        * (1 + COALESCE(produto.percentualIpiEntrada, 0) / 100),
                        2
                    ) <= :precoMaximo
              )
              AND (
                    :categoriaCodigo IS NULL
                    OR categoria.codigo = :categoriaCodigo
                    OR categoria.codigo LIKE CONCAT(:categoriaCodigo, '.%')
              )
              AND (
                    :busca IS NULL
                    OR LOWER(produto.nomeExibidoSite) LIKE CONCAT('%', LOWER(:busca), '%')
                    OR LOWER(produto.marca) LIKE CONCAT('%', LOWER(:busca), '%')
                    OR LOWER(produto.codigoSantri) LIKE CONCAT('%', LOWER(:busca), '%')
              )
            ORDER BY produto.nomeExibidoSite ASC, produto.codigoSantri ASC
            """,
            countQuery = """
            SELECT COUNT(produto)
            FROM Produto produto
            JOIN produto.categoria categoria
            WHERE produto.exibirNoSite = true
              AND produto.disponivelUltimaImportacao = true
              AND (:somenteDestaques = false OR produto.destaqueNaHome = true)
              AND (
                    :precoMinimo IS NULL
                    OR ROUND(
                        produto.precoSemIpi
                        * (1 + COALESCE(produto.percentualIpiEntrada, 0) / 100),
                        2
                    ) >= :precoMinimo
              )
              AND (
                    :precoMaximo IS NULL
                    OR ROUND(
                        produto.precoSemIpi
                        * (1 + COALESCE(produto.percentualIpiEntrada, 0) / 100),
                        2
                    ) <= :precoMaximo
              )
              AND (
                    :categoriaCodigo IS NULL
                    OR categoria.codigo = :categoriaCodigo
                    OR categoria.codigo LIKE CONCAT(:categoriaCodigo, '.%')
              )
              AND (
                    :busca IS NULL
                    OR LOWER(produto.nomeExibidoSite) LIKE CONCAT('%', LOWER(:busca), '%')
                    OR LOWER(produto.marca) LIKE CONCAT('%', LOWER(:busca), '%')
                    OR LOWER(produto.codigoSantri) LIKE CONCAT('%', LOWER(:busca), '%')
              )
            """
    )
    Page<Produto> findCatalogoInternoPorCategoria(
            @Param("categoriaCodigo") String categoriaCodigo,
            @Param("busca") String busca,
            @Param("somenteDestaques") boolean somenteDestaques,
            @Param("precoMinimo") BigDecimal precoMinimo,
            @Param("precoMaximo") BigDecimal precoMaximo,
            Pageable pageable
    );

    default Page<Produto> findCatalogoInternoPorCategoria(
            String categoriaCodigo,
            String busca,
            boolean somenteDestaques,
            Pageable pageable
    ) {
        return findCatalogoInternoPorCategoria(
                categoriaCodigo,
                busca,
                somenteDestaques,
                null,
                null,
                pageable
        );
    }
}
