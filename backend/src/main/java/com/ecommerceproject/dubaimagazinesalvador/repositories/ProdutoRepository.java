package com.ecommerceproject.dubaimagazinesalvador.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ecommerceproject.dubaimagazinesalvador.domain.produto.Produto;

@Repository
public interface ProdutoRepository extends JpaRepository<Produto, String> {

    @Query(
            value = """
            SELECT produto
            FROM Produto produto
            JOIN FETCH produto.categoria categoria
            WHERE (:incluirOcultos = true OR produto.exibirNoSite = true)
              AND (:somenteDestaques = false OR produto.destaqueNaHome = true)
              AND (
                    :categoriaCodigo IS NULL
                    OR categoria.codigo = :categoriaCodigo
                    OR categoria.codigo LIKE CONCAT(:categoriaCodigo, '.%')
              )
              AND (
                    :busca IS NULL
                    OR LOWER(produto.nomeExibidoSite) LIKE CONCAT('%', LOWER(:busca), '%')
                    OR LOWER(produto.descricao) LIKE CONCAT('%', LOWER(:busca), '%')
                    OR LOWER(produto.codigoSantri) LIKE CONCAT('%', LOWER(:busca), '%')
                    OR LOWER(produto.codigoOriginal) LIKE CONCAT('%', LOWER(:busca), '%')
                    OR LOWER(produto.marca) LIKE CONCAT('%', LOWER(:busca), '%')
              )
            ORDER BY produto.nomeExibidoSite ASC
            """,
            countQuery = """
            SELECT COUNT(produto)
            FROM Produto produto
            JOIN produto.categoria categoria
            WHERE (:incluirOcultos = true OR produto.exibirNoSite = true)
              AND (:somenteDestaques = false OR produto.destaqueNaHome = true)
              AND (
                    :categoriaCodigo IS NULL
                    OR categoria.codigo = :categoriaCodigo
                    OR categoria.codigo LIKE CONCAT(:categoriaCodigo, '.%')
              )
              AND (
                    :busca IS NULL
                    OR LOWER(produto.nomeExibidoSite) LIKE CONCAT('%', LOWER(:busca), '%')
                    OR LOWER(produto.descricao) LIKE CONCAT('%', LOWER(:busca), '%')
                    OR LOWER(produto.codigoSantri) LIKE CONCAT('%', LOWER(:busca), '%')
                    OR LOWER(produto.codigoOriginal) LIKE CONCAT('%', LOWER(:busca), '%')
                    OR LOWER(produto.marca) LIKE CONCAT('%', LOWER(:busca), '%')
              )
            """
    )
    Page<Produto> findCatalogoPorCategoria(
            @Param("categoriaCodigo") String categoriaCodigo,
            @Param("busca") String busca,
            @Param("incluirOcultos") boolean incluirOcultos,
            @Param("somenteDestaques") boolean somenteDestaques,
            Pageable pageable
    );
}
