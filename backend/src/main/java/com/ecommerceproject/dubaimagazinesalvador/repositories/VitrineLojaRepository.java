package com.ecommerceproject.dubaimagazinesalvador.repositories;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ecommerceproject.dubaimagazinesalvador.domain.vitrine.loja.VitrineLoja;

public interface VitrineLojaRepository extends JpaRepository<VitrineLoja, Long> {

    @Query(
            value = """
            SELECT DISTINCT vitrine
            FROM VitrineLoja vitrine
            JOIN vitrine.opcoes opcao
            JOIN opcao.produto produto
            LEFT JOIN opcao.secoes secao
            WHERE (:somenteAtivas = false OR vitrine.ativo = true)
              AND (
                    :busca IS NULL
                    OR LOWER(produto.nomeExibidoSite) LIKE CONCAT('%', LOWER(:busca), '%')
                    OR LOWER(produto.codigoSantri) LIKE CONCAT('%', LOWER(:busca), '%')
                    OR LOWER(produto.codigoOriginal) LIKE CONCAT('%', LOWER(:busca), '%')
                    OR LOWER(produto.marca) LIKE CONCAT('%', LOWER(:busca), '%')
                    OR LOWER(opcao.rotuloOpcao) LIKE CONCAT('%', LOWER(:busca), '%')
                    OR LOWER(secao.titulo) LIKE CONCAT('%', LOWER(:busca), '%')
                    OR LOWER(secao.conteudo) LIKE CONCAT('%', LOWER(:busca), '%')
              )
            ORDER BY vitrine.atualizadoEm DESC, vitrine.id DESC
            """,
            countQuery = """
            SELECT COUNT(DISTINCT vitrine.id)
            FROM VitrineLoja vitrine
            JOIN vitrine.opcoes opcao
            JOIN opcao.produto produto
            LEFT JOIN opcao.secoes secao
            WHERE (:somenteAtivas = false OR vitrine.ativo = true)
              AND (
                    :busca IS NULL
                    OR LOWER(produto.nomeExibidoSite) LIKE CONCAT('%', LOWER(:busca), '%')
                    OR LOWER(produto.codigoSantri) LIKE CONCAT('%', LOWER(:busca), '%')
                    OR LOWER(produto.codigoOriginal) LIKE CONCAT('%', LOWER(:busca), '%')
                    OR LOWER(produto.marca) LIKE CONCAT('%', LOWER(:busca), '%')
                    OR LOWER(opcao.rotuloOpcao) LIKE CONCAT('%', LOWER(:busca), '%')
                    OR LOWER(secao.titulo) LIKE CONCAT('%', LOWER(:busca), '%')
                    OR LOWER(secao.conteudo) LIKE CONCAT('%', LOWER(:busca), '%')
              )
            """
    )
    Page<VitrineLoja> pesquisar(
            @Param("somenteAtivas") boolean somenteAtivas,
            @Param("busca") String busca,
            Pageable pageable
    );

    Optional<VitrineLoja> findByIdAndAtivoTrue(Long id);

    @Query("""
            SELECT produto.codigoSantri
            FROM VitrineLoja vitrine
            JOIN vitrine.opcoes opcao
            JOIN opcao.produto produto
            WHERE produto.codigoSantri IN :codigos
              AND (:vitrineId IS NULL OR vitrine.id <> :vitrineId)
            """)
    List<String> encontrarProdutosEmOutrasVitrines(
            @Param("codigos") Collection<String> codigos,
            @Param("vitrineId") Long vitrineId
    );
}

