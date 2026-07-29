package com.ecommerceproject.dubaimagazinesalvador.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ecommerceproject.dubaimagazinesalvador.domain.categoria.Categoria;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, String> {

    List<Categoria> findByExibirNoSiteTrueOrderByCaminhoAsc();

    List<Categoria> findByNivelOrderByNomeAsc(int nivel);

    List<Categoria> findByNivelAndExibirNoSiteTrueOrderByNomeAsc(int nivel);

    List<Categoria> findByCategoriaPai_CodigoOrderByNomeAsc(String categoriaPaiCodigo);

    List<Categoria> findByCategoriaPai_CodigoAndExibirNoSiteTrueOrderByNomeAsc(
            String categoriaPaiCodigo
    );
}
