package com.ecommerceproject.dubaimagazinesalvador.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ecommerceproject.dubaimagazinesalvador.domain.usuarios.Usuario;

import jakarta.persistence.LockModeType;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findFirstByCodigoSantriIgnoreCase(String codigoSantri);

    boolean existsByCodigoSantriIgnoreCase(String codigoSantri);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT usuario
            FROM Usuario usuario
            WHERE LOWER(usuario.codigoSantri) = LOWER(:codigoSantri)
            """)
    Optional<Usuario> buscarPorCodigoSantriParaAtualizacao(
            @Param("codigoSantri") String codigoSantri
    );
}
