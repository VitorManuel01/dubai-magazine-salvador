package com.ecommerceproject.dubaimagazinesalvador.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import com.ecommerceproject.dubaimagazinesalvador.domain.usuarios.Usuario;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {
    Optional<Usuario> findByLogin(String login);
    Optional<Usuario> findByEmail(String email);
    Optional<Usuario> findFirstByLoginIgnoreCase(String login);
    Optional<Usuario> findFirstByEmailIgnoreCase(String email);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Usuario> findFirstByLoginIgnoreCaseOrEmailIgnoreCase(
            String login,
            String email
    );
    boolean existsByLoginIgnoreCase(String login);
    boolean existsByEmailIgnoreCase(String email);
}
