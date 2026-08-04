package com.ecommerceproject.dubaimagazinesalvador.controller;

import java.util.List;
import java.util.Locale;

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

import com.ecommerceproject.dubaimagazinesalvador.domain.usuarios.Funcionario;
import com.ecommerceproject.dubaimagazinesalvador.domain.usuarios.FuncionarioRequestDTO;
import com.ecommerceproject.dubaimagazinesalvador.domain.usuarios.FuncionarioResponseDTO;
import com.ecommerceproject.dubaimagazinesalvador.repositories.FuncionarioRepository;
import com.ecommerceproject.dubaimagazinesalvador.repositories.UsuarioRepository;

import jakarta.validation.Valid;

@RestController
@RequestMapping("funcionario")
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

    @PostMapping
    public ResponseEntity<FuncionarioResponseDTO> saveFuncionario(
            @Valid @RequestBody FuncionarioRequestDTO data
    ) {
        String codigoSantri = data.codigoSantri().trim().toUpperCase(Locale.ROOT);
        if (usuarioRepository.existsByCodigoSantriIgnoreCase(codigoSantri)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Código Santri já cadastrado"
            );
        }

        Funcionario funcionario = new Funcionario(
                data,
                passwordEncoder.encode(data.senha())
        );
        funcionario.setCodigoSantri(codigoSantri);
        funcionario = repository.save(funcionario);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new FuncionarioResponseDTO(funcionario));
    }

    @GetMapping
    public List<FuncionarioResponseDTO> getAll() {
        return repository.findAll().stream()
                .map(FuncionarioResponseDTO::new)
                .toList();
    }
}
