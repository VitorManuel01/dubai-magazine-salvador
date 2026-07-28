package com.ecommerceproject.dubaimagazinesalvador.services;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.ecommerceproject.dubaimagazinesalvador.domain.usuarios.Usuario;
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

        // 2. Se não for admin, tenta encontrar como Usuário comum (por login ou email)
           // Busca o usuário pelo email
           Optional<Usuario> usuarioByEmail = usuarioRepository.findByEmail(username);
       
           // Busca o usuário pelo login
           Optional<Usuario> usuarioByLogin = usuarioRepository.findByLogin(username);
       
           // Verifica se algum dos dois retornou um usuário e retorna o primeiro encontrado
           if (usuarioByEmail.isPresent()) {
               return usuarioByEmail.get();
           } else if (usuarioByLogin.isPresent()) {
               return usuarioByLogin.get();
           }

        // 3. Se não encontrou em nenhum dos repositórios, lança a exceção.
        throw new UsernameNotFoundException("Usuário '" + username + "' não encontrado em nenhuma base de dados.");
    }
}


