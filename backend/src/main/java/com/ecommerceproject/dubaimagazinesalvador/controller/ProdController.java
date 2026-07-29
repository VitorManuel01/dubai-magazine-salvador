package com.ecommerceproject.dubaimagazinesalvador.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.ecommerceproject.dubaimagazinesalvador.domain.categoria.Categoria;
import com.ecommerceproject.dubaimagazinesalvador.domain.produto.Produto;
import com.ecommerceproject.dubaimagazinesalvador.domain.produto.ProdutoRequestDTO;
import com.ecommerceproject.dubaimagazinesalvador.domain.produto.ProdutoResponseDTO;
import com.ecommerceproject.dubaimagazinesalvador.repositories.CategoriaRepository;
import com.ecommerceproject.dubaimagazinesalvador.repositories.ProdutoRepository;
import com.ecommerceproject.dubaimagazinesalvador.services.produto.ApresentacaoProdutoService;

@RestController
@RequestMapping("produto")
public class ProdController {

    private final ProdutoRepository produtoRepository;
    private final CategoriaRepository categoriaRepository;
    private final ApresentacaoProdutoService apresentacaoProdutoService;

    public ProdController(
            ProdutoRepository produtoRepository,
            CategoriaRepository categoriaRepository,
            ApresentacaoProdutoService apresentacaoProdutoService
    ) {
        this.produtoRepository = produtoRepository;
        this.categoriaRepository = categoriaRepository;
        this.apresentacaoProdutoService = apresentacaoProdutoService;
    }

    @PostMapping
    public ResponseEntity<ProdutoResponseDTO> saveProduto(@RequestBody ProdutoRequestDTO data) {
        if (data.codigoSantri() == null || data.codigoSantri().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O código Santri é obrigatório.");
        }
        if (produtoRepository.existsById(data.codigoSantri())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Produto já cadastrado.");
        }

        Categoria categoria = buscarCategoria(data.categoriaCodigo());
        Produto produto = produtoRepository.save(new Produto(data, categoria));
        return ResponseEntity.status(HttpStatus.CREATED).body(new ProdutoResponseDTO(produto));
    }

    @GetMapping
    @Transactional(readOnly = true)
    public Page<ProdutoResponseDTO> getAll(
            @RequestParam(required = false) String categoriaCodigo,
            @RequestParam(required = false) String busca,
            @RequestParam(defaultValue = "false") boolean somenteDestaques,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "24") int tamanho,
            Authentication authentication
    ) {
        if (pagina < 0 || tamanho < 1 || tamanho > 60) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A página deve ser positiva e o tamanho deve estar entre 1 e 60."
            );
        }
        String categoriaNormalizada = categoriaCodigo == null || categoriaCodigo.isBlank()
                ? null
                : categoriaCodigo.trim();
        String buscaNormalizada = busca == null || busca.isBlank()
                ? null
                : busca.trim();
        boolean incluirOcultos = authentication != null
                && authentication.getAuthorities().stream()
                        .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));

        return produtoRepository.findCatalogoPorCategoria(
                categoriaNormalizada,
                buscaNormalizada,
                incluirOcultos,
                somenteDestaques,
                PageRequest.of(pagina, tamanho)
        ).map(ProdutoResponseDTO::new);
    }

    @DeleteMapping("/{codigoSantri}")
    public ResponseEntity<String> deleteProduto(@PathVariable String codigoSantri) {
        return produtoRepository.findById(codigoSantri)
                .map(produto -> {
                    produtoRepository.delete(produto);
                    return ResponseEntity.ok("Produto deletado com sucesso!");
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping(
            value = "/{codigoSantri}/apresentacao",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ProdutoResponseDTO> updateApresentacao(
            @PathVariable String codigoSantri,
            @RequestParam(required = false) String nomeExibidoSite,
            @RequestParam boolean exibirNoSite,
            @RequestParam boolean destaqueNaHome,
            @RequestParam(required = false) MultipartFile imagem
    ) {
        return ResponseEntity.ok(
                apresentacaoProdutoService.atualizar(
                        codigoSantri,
                        nomeExibidoSite,
                        exibirNoSite,
                        destaqueNaHome,
                        imagem
                )
        );
    }

    private Categoria buscarCategoria(String categoriaCodigo) {
        if (categoriaCodigo == null || categoriaCodigo.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A categoria é obrigatória.");
        }
        return categoriaRepository.findById(categoriaCodigo)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Categoria não encontrada: " + categoriaCodigo
                ));
    }
}
