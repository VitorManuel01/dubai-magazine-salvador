package com.ecommerceproject.dubaimagazinesalvador.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerceproject.dubaimagazinesalvador.domain.vitrine.VitrineHomeRequestDTO;
import com.ecommerceproject.dubaimagazinesalvador.domain.vitrine.VitrineHomeResponseDTO;
import com.ecommerceproject.dubaimagazinesalvador.services.vitrine.VitrineHomeService;

@RestController
public class VitrineHomeController {

    private final VitrineHomeService vitrineHomeService;

    public VitrineHomeController(VitrineHomeService vitrineHomeService) {
        this.vitrineHomeService = vitrineHomeService;
    }

    @GetMapping("/vitrines-home")
    public List<VitrineHomeResponseDTO> listarPublicas() {
        return vitrineHomeService.listarPublicas();
    }

    @GetMapping("/admin/vitrines-home")
    public List<VitrineHomeResponseDTO> listarAdministracao() {
        return vitrineHomeService.listarAdministracao();
    }

    @PostMapping("/admin/vitrines-home")
    public ResponseEntity<VitrineHomeResponseDTO> criar(
            @RequestBody VitrineHomeRequestDTO dados
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vitrineHomeService.criar(dados));
    }

    @PutMapping("/admin/vitrines-home/{id}")
    public VitrineHomeResponseDTO atualizar(
            @PathVariable Long id,
            @RequestBody VitrineHomeRequestDTO dados
    ) {
        return vitrineHomeService.atualizar(id, dados);
    }

    @DeleteMapping("/admin/vitrines-home/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        vitrineHomeService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
