package com.ecommerceproject.dubaimagazinesalvador.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ecommerceproject.dubaimagazinesalvador.domain.importacao.ImportacaoProdutosResponseDTO;
import com.ecommerceproject.dubaimagazinesalvador.domain.importacao.ImportacaoPromocoesResponseDTO;
import com.ecommerceproject.dubaimagazinesalvador.domain.importacao.ImportacaoEstoqueResponseDTO;
import com.ecommerceproject.dubaimagazinesalvador.services.importacao.ControleImportacaoProdutosService;
import com.ecommerceproject.dubaimagazinesalvador.services.importacao.ImportacaoEstoqueService;
import com.ecommerceproject.dubaimagazinesalvador.services.importacao.ImportacaoProdutosService;
import com.ecommerceproject.dubaimagazinesalvador.services.importacao.ImportacaoPromocoesService;

@RestController
@RequestMapping("/admin/importacoes")
public class ImportacaoProdutosController {

    private final ImportacaoProdutosService importacaoProdutosService;
    private final ImportacaoPromocoesService importacaoPromocoesService;
    private final ImportacaoEstoqueService importacaoEstoqueService;
    private final ControleImportacaoProdutosService controleImportacao;

    public ImportacaoProdutosController(
            ImportacaoProdutosService importacaoProdutosService,
            ImportacaoPromocoesService importacaoPromocoesService,
            ImportacaoEstoqueService importacaoEstoqueService,
            ControleImportacaoProdutosService controleImportacao
    ) {
        this.importacaoProdutosService = importacaoProdutosService;
        this.importacaoPromocoesService = importacaoPromocoesService;
        this.importacaoEstoqueService = importacaoEstoqueService;
        this.controleImportacao = controleImportacao;
    }

    @PostMapping(value = "/produtos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportacaoProdutosResponseDTO> importar(
            @RequestParam("arquivo") MultipartFile arquivo
    ) {
        return ResponseEntity.ok(controleImportacao.executarExclusivamente(
                () -> importacaoProdutosService.importar(arquivo)
        ));
    }

    @PostMapping(value = "/promocoes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportacaoPromocoesResponseDTO> importarPromocoes(
            @RequestParam("arquivo") MultipartFile arquivo
    ) {
        return ResponseEntity.ok(controleImportacao.executarExclusivamente(
                () -> importacaoPromocoesService.importar(arquivo)
        ));
    }

    @PostMapping(value = "/estoque", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportacaoEstoqueResponseDTO> importarEstoque(
            @RequestParam("arquivo") MultipartFile arquivo
    ) {
        return ResponseEntity.ok(controleImportacao.executarExclusivamente(
                () -> importacaoEstoqueService.importar(arquivo)
        ));
    }
}
