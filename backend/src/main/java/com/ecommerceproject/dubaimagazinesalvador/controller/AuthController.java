package com.ecommerceproject.dubaimagazinesalvador.controller;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerceproject.dubaimagazinesalvador.domain.usuarios.Administrador;
import com.ecommerceproject.dubaimagazinesalvador.domain.usuarios.AuthenticationDTO;
import com.ecommerceproject.dubaimagazinesalvador.domain.usuarios.LoginResponseDTO;
import com.ecommerceproject.dubaimagazinesalvador.domain.usuarios.RegisterAdmDTO;
import com.ecommerceproject.dubaimagazinesalvador.domain.usuarios.Usuario;
import com.ecommerceproject.dubaimagazinesalvador.infra.security.TokenService;
import com.ecommerceproject.dubaimagazinesalvador.repositories.AdministradorRespository;
import com.ecommerceproject.dubaimagazinesalvador.services.TentativasLoginService;
import com.ecommerceproject.dubaimagazinesalvador.services.TentativasLoginService.EstadoBloqueio;

@RestController
@RequestMapping("auth")
// @CrossOrigin(origins = "http://localhost:5173",  allowCredentials = "true")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private AdministradorRespository admRepository;
    @Autowired
    private TokenService tokenService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private TentativasLoginService tentativasLoginService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthenticationDTO data) {
        String identificador = data.emailOrLogin().trim();
        EstadoBloqueio bloqueioAtual = tentativasLoginService.verificarBloqueio(identificador);
        if (bloqueioAtual.bloqueado()) {
            return respostaBloqueada(bloqueioAtual.bloqueadoAte());
        }

        try {
            var usernamePassword = new UsernamePasswordAuthenticationToken(identificador, data.senha());
            var auth = this.authenticationManager.authenticate(usernamePassword);
            Usuario usuario = (Usuario) auth.getPrincipal();
            tentativasLoginService.registrarSucesso(usuario.getId());
            var token = tokenService.generateToken(usuario);
            return ResponseEntity.ok(new LoginResponseDTO(token));
        } catch (AuthenticationException e) {
            EstadoBloqueio bloqueio = tentativasLoginService.registrarFalha(identificador);
            if (bloqueio.bloqueado()) {
                return respostaBloqueada(bloqueio.bloqueadoAte());
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    new LoginErroDTO("Login ou senha inválidos.", null)
            );
        }
    }

    private ResponseEntity<LoginErroDTO> respostaBloqueada(Instant bloqueadoAte) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(
                new LoginErroDTO(
                        "Muitas tentativas de login. Tente novamente em 20 minutos.",
                        bloqueadoAte
                )
        );
    }

    public record LoginErroDTO(String erro, Instant bloqueadoAte) {
    }
    


    @PostMapping("/registerADM")
    public ResponseEntity<String>  registerADM(@RequestBody @Validated RegisterAdmDTO dataAdm) {
        if (this.admRepository.findByEmail(dataAdm.email()) != null) {
            return ResponseEntity.badRequest().build();
        } else if (this.admRepository.findByLogin(dataAdm.login()) != null) {
            return ResponseEntity.badRequest().build();
        }

        String encryptedPassword = passwordEncoder.encode(dataAdm.senha());

        // Criar um novo Administrador, que é um Usuario
        Administrador novoAdministrador = new Administrador();
        novoAdministrador.setLogin(dataAdm.login());
        novoAdministrador.setEmail(dataAdm.email());
        novoAdministrador.setSenha(encryptedPassword);
        novoAdministrador.setFuncao(dataAdm.funcao());
        novoAdministrador.setAdmin(dataAdm.admin());
        novoAdministrador.setNome(dataAdm.nome());
        novoAdministrador.setCPF(dataAdm.CPF());
        novoAdministrador.setSexo(dataAdm.sexo());
        novoAdministrador.setDataNascimento(dataAdm.dataNascimento());
        novoAdministrador.setCEP(dataAdm.CEP());
        novoAdministrador.setEndereco(dataAdm.endereco());
        novoAdministrador.setBairro(dataAdm.bairro());
        novoAdministrador.setTelefone(dataAdm.telefone());

        // Salvar o novo Administrador (que também será salvo na tabela usuarios)
        this.admRepository.save(novoAdministrador);

        return ResponseEntity.ok().build();
    }
}
