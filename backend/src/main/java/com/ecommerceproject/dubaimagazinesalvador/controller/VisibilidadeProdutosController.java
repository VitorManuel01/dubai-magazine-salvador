package com.ecommerceproject.dubaimagazinesalvador.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerceproject.dubaimagazinesalvador.domain.produto.VisibilidadeProdutosRequestDTO;
import com.ecommerceproject.dubaimagazinesalvador.domain.produto.VisibilidadeProdutosResponseDTO;
import com.ecommerceproject.dubaimagazinesalvador.services.produto.VisibilidadeProdutosService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/admin/produtos")
public class VisibilidadeProdutosController {

    private final VisibilidadeProdutosService service;

    public VisibilidadeProdutosController(VisibilidadeProdutosService service) {
        this.service = service;
    }

    @PutMapping("/visibilidade")
    public ResponseEntity<VisibilidadeProdutosResponseDTO> alterarVisibilidade(
            @Valid @RequestBody VisibilidadeProdutosRequestDTO requisicao
    ) {
        return ResponseEntity.ok(service.alterarVisibilidade(requisicao));
    }
}
