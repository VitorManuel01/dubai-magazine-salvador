package com.ecommerceproject.dubaimagazinesalvador.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerceproject.dubaimagazinesalvador.domain.vitrine.AtualizacaoDepoimentosHomeRequestDTO;
import com.ecommerceproject.dubaimagazinesalvador.domain.vitrine.DepoimentoHomeResponseDTO;
import com.ecommerceproject.dubaimagazinesalvador.services.vitrine.DepoimentoHomeService;

import jakarta.validation.Valid;

@RestController
public class DepoimentoHomeController {

    private final DepoimentoHomeService service;

    public DepoimentoHomeController(DepoimentoHomeService service) {
        this.service = service;
    }

    @GetMapping("/depoimentos-home")
    public List<DepoimentoHomeResponseDTO> listar() {
        return service.listar();
    }

    @PutMapping("/admin/depoimentos-home")
    public List<DepoimentoHomeResponseDTO> atualizar(
            @Valid @RequestBody AtualizacaoDepoimentosHomeRequestDTO request
    ) {
        return service.atualizar(request.depoimentos());
    }
}
