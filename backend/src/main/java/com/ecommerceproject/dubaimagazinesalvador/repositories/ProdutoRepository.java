package com.ecommerceproject.dubaimagazinesalvador.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ecommerceproject.dubaimagazinesalvador.domain.produto.Produto;

@Repository
public interface ProdutoRepository extends JpaRepository<Produto, String> {
    
}
