package com.ecommerceproject.dubaimagazinesalvador.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ecommerceproject.dubaimagazinesalvador.domain.usuarios.Funcionario;

@Repository
public interface FuncionarioRepository extends JpaRepository<Funcionario, UUID>{

}
