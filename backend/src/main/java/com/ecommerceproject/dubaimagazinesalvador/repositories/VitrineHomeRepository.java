package com.ecommerceproject.dubaimagazinesalvador.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecommerceproject.dubaimagazinesalvador.domain.vitrine.VitrineHome;

public interface VitrineHomeRepository extends JpaRepository<VitrineHome, Long> {

    List<VitrineHome> findByAtivoTrueOrderByOrdemAscIdAsc();

    List<VitrineHome> findAllByOrderByOrdemAscIdAsc();

    boolean existsByCategoria_Codigo(String categoriaCodigo);

    boolean existsByCategoria_CodigoAndIdNot(String categoriaCodigo, Long id);
}
