package com.ecommerceproject.dubaimagazinesalvador.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ecommerceproject.dubaimagazinesalvador.domain.importacao.ImportacaoProdutosResponseDTO;
import com.ecommerceproject.dubaimagazinesalvador.services.importacao.ImportacaoProdutosService;

@RestController
@RequestMapping("/admin/importacoes")
public class ImportacaoProdutosController {

    private final ImportacaoProdutosService importacaoProdutosService;

    public ImportacaoProdutosController(ImportacaoProdutosService importacaoProdutosService) {
        this.importacaoProdutosService = importacaoProdutosService;
    }

    @PostMapping(value = "/produtos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportacaoProdutosResponseDTO> importar(
            @RequestParam("arquivo") MultipartFile arquivo
    ) {
        return ResponseEntity.ok(importacaoProdutosService.importar(arquivo));
    }
}
