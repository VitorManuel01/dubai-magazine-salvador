package com.ecommerceproject.dubaimagazinesalvador.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecommerceproject.dubaimagazinesalvador.domain.vitrine.DepoimentoHome;

public interface DepoimentoHomeRepository extends JpaRepository<DepoimentoHome, Integer> {

    List<DepoimentoHome> findAllByOrderByPosicaoAsc();
}
