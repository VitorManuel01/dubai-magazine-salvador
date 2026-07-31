package com.ecommerceproject.dubaimagazinesalvador.services;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.ecommerceproject.dubaimagazinesalvador.domain.usuarios.Usuario;
import com.ecommerceproject.dubaimagazinesalvador.domain.usuarios.Role;
import com.ecommerceproject.dubaimagazinesalvador.repositories.AdministradorRespository; // Importar
import com.ecommerceproject.dubaimagazinesalvador.repositories.UsuarioRepository; // Você já tem

@Service // Anotação @Primary para garantir que este seja o bean principal, caso haja outros.
public class AuthorizationService implements UserDetailsService {

    @Autowired
    UsuarioRepository usuarioRepository;

    @Autowired
    AdministradorRespository administradorRespository; // Injetar o novo repositório

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. Tenta encontrar como Administrador primeiro (por login ou email)
        UserDetails admin = administradorRespository.findByLogin(username);
        if (admin == null) {
            admin = administradorRespository.findByEmail(username);
        }
        
        if (admin != null) {
            return admin;
        }

        // O catálogo não possui mais autenticação de clientes. Somente funcionários
        // podem passar pela busca genérica de usuários.
        Optional<Usuario> funcionario = usuarioRepository
                .findFirstByEmailIgnoreCase(username)
                .or(() -> usuarioRepository.findFirstByLoginIgnoreCase(username))
                .filter(usuario -> usuario.getFuncao() == Role.ROLE_FUNCIONARIO);

        if (funcionario.isPresent()) {
            return funcionario.get();
        }

        // 3. Se não encontrou em nenhum dos repositórios, lança a exceção.
        throw new UsernameNotFoundException("Usuário '" + username + "' não encontrado em nenhuma base de dados.");
    }
}


