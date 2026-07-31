package com.ecommerceproject.dubaimagazinesalvador.controller;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerceproject.dubaimagazinesalvador.domain.vitrine.loja.VitrineLojaRequestDTO;
import com.ecommerceproject.dubaimagazinesalvador.domain.vitrine.loja.VitrineLojaResponseDTO;
import com.ecommerceproject.dubaimagazinesalvador.domain.vitrine.loja.ProdutoCandidatoVitrineLojaDTO;
import com.ecommerceproject.dubaimagazinesalvador.services.vitrine.VitrineLojaService;

@RestController
public class VitrineLojaController {

    private final VitrineLojaService vitrineLojaService;

    public VitrineLojaController(VitrineLojaService vitrineLojaService) {
        this.vitrineLojaService = vitrineLojaService;
    }

    @GetMapping("/vitrine-loja")
    public Page<VitrineLojaResponseDTO> pesquisar(
            @RequestParam(required = false) String busca,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "12") int tamanho
    ) {
        return vitrineLojaService.pesquisar(busca, pagina, tamanho, true);
    }

    @GetMapping("/vitrine-loja/{id}")
    public VitrineLojaResponseDTO buscar(@PathVariable Long id) {
        return vitrineLojaService.buscarAtiva(id);
    }

    @GetMapping("/admin/vitrine-loja")
    public Page<VitrineLojaResponseDTO> pesquisarAdministracao(
            @RequestParam(required = false) String busca,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho
    ) {
        return vitrineLojaService.pesquisar(busca, pagina, tamanho, false);
    }

    @GetMapping("/admin/vitrine-loja/produtos")
    public Page<ProdutoCandidatoVitrineLojaDTO> pesquisarProdutosParaAdministracao(
            @RequestParam(required = false) String busca,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "60") int tamanho
    ) {
        return vitrineLojaService.pesquisarProdutosParaAdministracao(
                busca,
                pagina,
                tamanho
        );
    }

    @GetMapping("/admin/vitrine-loja/{id}")
    public VitrineLojaResponseDTO buscarAdministracao(@PathVariable Long id) {
        return vitrineLojaService.buscarAdministracao(id);
    }

    @PostMapping("/admin/vitrine-loja")
    public ResponseEntity<VitrineLojaResponseDTO> criar(
            @RequestBody VitrineLojaRequestDTO dados
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(vitrineLojaService.criar(dados));
    }

    @PutMapping("/admin/vitrine-loja/{id}")
    public VitrineLojaResponseDTO atualizar(
            @PathVariable Long id,
            @RequestBody VitrineLojaRequestDTO dados
    ) {
        return vitrineLojaService.atualizar(id, dados);
    }

    @DeleteMapping("/admin/vitrine-loja/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        vitrineLojaService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
