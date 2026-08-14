package com.ecommerceproject.dubaimagazinesalvador.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerceproject.dubaimagazinesalvador.domain.produto.ExclusaoProdutosRequestDTO;
import com.ecommerceproject.dubaimagazinesalvador.domain.produto.ExclusaoProdutosResponseDTO;
import com.ecommerceproject.dubaimagazinesalvador.services.produto.ExclusaoProdutoService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/admin/produtos")
public class ExclusaoProdutosController {

    private final ExclusaoProdutoService exclusaoProdutoService;

    public ExclusaoProdutosController(ExclusaoProdutoService exclusaoProdutoService) {
        this.exclusaoProdutoService = exclusaoProdutoService;
    }

    @DeleteMapping
    public ResponseEntity<ExclusaoProdutosResponseDTO> excluirSelecionados(
            @Valid @RequestBody ExclusaoProdutosRequestDTO requisicao
    ) {
        int excluidos = exclusaoProdutoService.excluirTodos(requisicao.codigosSantri());
        return ResponseEntity.ok(new ExclusaoProdutosResponseDTO(excluidos));
    }
}
