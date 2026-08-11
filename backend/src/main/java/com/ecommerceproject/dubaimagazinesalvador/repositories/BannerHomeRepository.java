package com.ecommerceproject.dubaimagazinesalvador.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecommerceproject.dubaimagazinesalvador.domain.vitrine.BannerHome;

public interface BannerHomeRepository extends JpaRepository<BannerHome, Integer> {

    List<BannerHome> findAllByOrderByPosicaoAsc();
}
