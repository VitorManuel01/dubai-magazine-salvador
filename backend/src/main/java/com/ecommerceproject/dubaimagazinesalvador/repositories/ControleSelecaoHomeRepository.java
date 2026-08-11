package com.ecommerceproject.dubaimagazinesalvador.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.ecommerceproject.dubaimagazinesalvador.domain.vitrine.ControleSelecaoHome;

import jakarta.persistence.LockModeType;

public interface ControleSelecaoHomeRepository
        extends JpaRepository<ControleSelecaoHome, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT controle FROM ControleSelecaoHome controle WHERE controle.id = 1")
    Optional<ControleSelecaoHome> bloquearParaAtualizacao();
}
