package com.ecommerceproject.dubaimagazinesalvador.controller;

import java.util.List;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.ecommerceproject.dubaimagazinesalvador.domain.usuarios.FuncionarioRequestDTO;
import com.ecommerceproject.dubaimagazinesalvador.domain.usuarios.Funcionario;
import com.ecommerceproject.dubaimagazinesalvador.domain.usuarios.FuncionarioResponseDTO;
import com.ecommerceproject.dubaimagazinesalvador.repositories.FuncionarioRepository;
import com.ecommerceproject.dubaimagazinesalvador.repositories.UsuarioRepository;

import jakarta.validation.Valid;


@RestController //Anotação para definir o controller
@RequestMapping("funcionario") //Anotação para para "mapear qual tabela/classe" se está trabalhando
@PreAuthorize("hasRole('ADMIN')")
public class FunController {

    private final FuncionarioRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public FunController(
            FuncionarioRepository repository,
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping //Anotação para realizar o Post e enviar os dados para o banco
    public ResponseEntity<FuncionarioResponseDTO> saveFuncionario(
            @Valid @RequestBody FuncionarioRequestDTO data
    ) {
        String login = data.login().trim();
        String email = data.email().trim();

        if (usuarioRepository.existsByLoginIgnoreCase(login)
                || usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Login ou e-mail já cadastrado"
            );
        }

        String senhaCriptografada = passwordEncoder.encode(data.senha());
        Funcionario funcionario = repository.save(
                new Funcionario(data, senhaCriptografada)
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new FuncionarioResponseDTO(funcionario));
    }

    @GetMapping
    public List<FuncionarioResponseDTO> getAll(){
        return repository.findAll().stream()
                .map(FuncionarioResponseDTO::new)
                .toList();
    }

}
