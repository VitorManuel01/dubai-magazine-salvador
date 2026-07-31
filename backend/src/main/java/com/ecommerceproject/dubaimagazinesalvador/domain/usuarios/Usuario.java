package com.ecommerceproject.dubaimagazinesalvador.domain.usuarios;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "usuarios")
@EqualsAndHashCode(of = "id")
public abstract class Usuario implements UserDetails {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "VARCHAR(36)")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;
    private String login;
    private String email;
    private String senha;
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private Role funcao;
    @Column(name = "tentativas_login_falhas", nullable = false)
    private int tentativasLoginFalhas;
    @Column(name = "bloqueado_ate")
    private Instant bloqueadoAte;
    
    public String getLogin() {
        return login;
    }
    public void setLogin(String login) {
        this.login = login;
    }
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public String getSenha() {
        return senha;
    }
    public void setSenha(String senha) {
        this.senha = senha;
    }
    public UUID getId() {
        return id;
    }
    public void setId(UUID id) {
        this.id = id;
    }

    public Usuario(String login, String email, String senha, Role funcao){
        this.login = login;
        this.email = email;
        this.senha = senha;
        this.funcao = funcao;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (funcao == null) {
            return List.of();
        }
        return List.of(new SimpleGrantedAuthority(funcao.name()));
    }

    @Override
    public String getPassword() {
        return this.senha;
    }

    @Override
    public String getUsername() {
        if(this.login != null){
            return this.login;
        }else{
            return this.email;
        }
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // Implementação básica
    }

    @Override
    public boolean isAccountNonLocked() {
        return bloqueadoAte == null || !Instant.now().isBefore(bloqueadoAte);
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // Implementação básica
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
    
}
