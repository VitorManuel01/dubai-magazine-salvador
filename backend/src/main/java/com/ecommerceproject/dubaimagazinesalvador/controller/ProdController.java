package com.ecommerceproject.dubaimagazinesalvador.controller;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.ecommerceproject.dubaimagazinesalvador.domain.categoria.Categoria;
import com.ecommerceproject.dubaimagazinesalvador.domain.produto.Produto;
import com.ecommerceproject.dubaimagazinesalvador.domain.produto.ProdutoCatalogoPublicoDTO;
import com.ecommerceproject.dubaimagazinesalvador.domain.produto.ProdutoCatalogoInternoDTO;
import com.ecommerceproject.dubaimagazinesalvador.domain.produto.ProdutoRequestDTO;
import com.ecommerceproject.dubaimagazinesalvador.domain.produto.ProdutoResponseDTO;
import com.ecommerceproject.dubaimagazinesalvador.domain.produto.ProdutoDetalhePublicoDTO;
import com.ecommerceproject.dubaimagazinesalvador.domain.produto.AtualizacaoDescricaoProdutoRequestDTO;
import com.ecommerceproject.dubaimagazinesalvador.domain.produto.ReordenacaoImagensProdutoRequestDTO;
import com.ecommerceproject.dubaimagazinesalvador.repositories.CategoriaRepository;
import com.ecommerceproject.dubaimagazinesalvador.repositories.ProdutoRepository;
import com.ecommerceproject.dubaimagazinesalvador.services.produto.ApresentacaoProdutoService;
import com.ecommerceproject.dubaimagazinesalvador.services.produto.ExclusaoProdutoService;
import com.ecommerceproject.dubaimagazinesalvador.services.produto.DetalheProdutoService;
import com.ecommerceproject.dubaimagazinesalvador.services.produto.FiltrosPesquisaCatalogo;

@RestController
public class ProdController {

    private static final int PAGINA_MAXIMA = 5_000;
    private static final BigDecimal PRECO_MAXIMO_FILTRO =
            new BigDecimal("9999999999999.99");

    private final ProdutoRepository produtoRepository;
    private final CategoriaRepository categoriaRepository;
    private final ApresentacaoProdutoService apresentacaoProdutoService;
    private final ExclusaoProdutoService exclusaoProdutoService;
    private final DetalheProdutoService detalheProdutoService;

    public ProdController(
            ProdutoRepository produtoRepository,
            CategoriaRepository categoriaRepository,
            ApresentacaoProdutoService apresentacaoProdutoService,
            ExclusaoProdutoService exclusaoProdutoService,
            DetalheProdutoService detalheProdutoService
    ) {
        this.produtoRepository = produtoRepository;
        this.categoriaRepository = categoriaRepository;
        this.apresentacaoProdutoService = apresentacaoProdutoService;
        this.exclusaoProdutoService = exclusaoProdutoService;
        this.detalheProdutoService = detalheProdutoService;
    }

    @PostMapping("/produto")
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

    @GetMapping("/produto")
    @Transactional(readOnly = true)
    public Page<ProdutoCatalogoPublicoDTO> getAll(
            @RequestParam(required = false) String categoriaCodigo,
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) BigDecimal precoMinimo,
            @RequestParam(required = false) BigDecimal precoMaximo,
            @RequestParam(defaultValue = "false") boolean somenteDestaques,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "24") int tamanho
    ) {
        return buscarCatalogo(
                categoriaCodigo,
                busca,
                precoMinimo,
                precoMaximo,
                somenteDestaques,
                pagina,
                tamanho,
                false,
                false
        ).map(ProdutoCatalogoPublicoDTO::new);
    }

    @GetMapping("/interno/produtos")
    @Transactional(readOnly = true)
    public Page<ProdutoCatalogoInternoDTO> getAllInterno(
            @RequestParam(required = false) String categoriaCodigo,
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) BigDecimal precoMinimo,
            @RequestParam(required = false) BigDecimal precoMaximo,
            @RequestParam(defaultValue = "false") boolean somenteDestaques,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "24") int tamanho
    ) {
        return buscarCatalogo(
                categoriaCodigo,
                busca,
                precoMinimo,
                precoMaximo,
                somenteDestaques,
                pagina,
                tamanho,
                false,
                true
        ).map(ProdutoCatalogoInternoDTO::new);
    }

    @GetMapping("/produto/{idPublico}")
    public ProdutoDetalhePublicoDTO buscarDetalhePublico(@PathVariable String idPublico) {
        return detalheProdutoService.buscarPublico(idPublico);
    }

    @GetMapping("/admin/produtos/{idPublico}/detalhe")
    public ProdutoResponseDTO buscarDetalheAdministracao(@PathVariable String idPublico) {
        return detalheProdutoService.buscarAdministracao(idPublico);
    }

    @PutMapping("/admin/produtos/{codigoSantri}/descricao")
    public ProdutoResponseDTO atualizarDescricao(
            @PathVariable String codigoSantri,
            @RequestBody AtualizacaoDescricaoProdutoRequestDTO dados
    ) {
        return detalheProdutoService.atualizarDescricao(
                codigoSantri,
                dados == null ? null : dados.descricao()
        );
    }

    @PostMapping(
            value = "/admin/produtos/{codigoSantri}/imagens",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ProdutoResponseDTO> adicionarImagem(
            @PathVariable String codigoSantri,
            @RequestParam("imagem") MultipartFile imagem
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(detalheProdutoService.adicionarImagem(codigoSantri, imagem));
    }

    @DeleteMapping("/admin/produtos/{codigoSantri}/imagens/{imagemId}")
    public ProdutoResponseDTO removerImagem(
            @PathVariable String codigoSantri,
            @PathVariable Long imagemId
    ) {
        return detalheProdutoService.removerImagem(codigoSantri, imagemId);
    }

    @PutMapping("/admin/produtos/{codigoSantri}/imagens/ordem")
    public ProdutoResponseDTO reordenarImagens(
            @PathVariable String codigoSantri,
            @RequestBody ReordenacaoImagensProdutoRequestDTO dados
    ) {
        return detalheProdutoService.reordenarImagens(
                codigoSantri,
                dados == null ? null : dados.imagens()
        );
    }

    @GetMapping("/admin/produtos")
    @Transactional(readOnly = true)
    public Page<ProdutoResponseDTO> getAllAdministracao(
            @RequestParam(required = false) String categoriaCodigo,
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) BigDecimal precoMinimo,
            @RequestParam(required = false) BigDecimal precoMaximo,
            @RequestParam(defaultValue = "false") boolean somenteDestaques,
            @RequestParam(defaultValue = "false") boolean apenasVisiveis,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "24") int tamanho
    ) {
        return buscarCatalogo(
                categoriaCodigo,
                busca,
                precoMinimo,
                precoMaximo,
                somenteDestaques,
                pagina,
                tamanho,
                !apenasVisiveis,
                true
        ).map(ProdutoResponseDTO::new);
    }

    private Page<Produto> buscarCatalogo(
            String categoriaCodigo,
            String busca,
            BigDecimal precoMinimo,
            BigDecimal precoMaximo,
            boolean somenteDestaques,
            int pagina,
            int tamanho,
            boolean incluirOcultos,
            boolean pesquisarCodigoSantri
    ) {
        if (pagina < 0 || pagina > PAGINA_MAXIMA || tamanho < 1 || tamanho > 60) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A página deve estar entre 0 e 5000 e o tamanho entre 1 e 60."
            );
        }
        if ((precoMinimo != null && precoMinimo.signum() < 0)
                || (precoMaximo != null && precoMaximo.signum() < 0)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Os preços mínimo e máximo não podem ser negativos."
            );
        }
        if (precoMinimo != null
                && precoMaximo != null
                && precoMinimo.compareTo(precoMaximo) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O preço mínimo não pode ser maior que o preço máximo."
            );
        }
        validarFormatoPreco(precoMinimo);
        validarFormatoPreco(precoMaximo);
        String categoriaNormalizada = FiltrosPesquisaCatalogo
                .normalizarCodigoCategoria(categoriaCodigo);
        String buscaNormalizada = FiltrosPesquisaCatalogo.normalizarBuscaParaLike(
                busca, pesquisarCodigoSantri ? 1 : 2
        );
        PageRequest paginacao = PageRequest.of(pagina, tamanho);
        if (pesquisarCodigoSantri && !incluirOcultos) {
            if (precoMinimo == null && precoMaximo == null) {
                return produtoRepository.findCatalogoInternoPorCategoria(
                        categoriaNormalizada,
                        buscaNormalizada,
                        somenteDestaques,
                        paginacao
                );
            }
            return produtoRepository.findCatalogoInternoPorCategoria(
                    categoriaNormalizada,
                    buscaNormalizada,
                    somenteDestaques,
                    precoMinimo,
                    precoMaximo,
                    paginacao
            );
        }
        if (precoMinimo == null && precoMaximo == null) {
            return produtoRepository.findCatalogoPorCategoria(
                    categoriaNormalizada,
                    buscaNormalizada,
                    incluirOcultos,
                    somenteDestaques,
                    paginacao
            );
        }
        return produtoRepository.findCatalogoPorCategoria(
                categoriaNormalizada,
                buscaNormalizada,
                incluirOcultos,
                somenteDestaques,
                precoMinimo,
                precoMaximo,
                paginacao
        );
    }

    private void validarFormatoPreco(BigDecimal preco) {
        if (preco == null) {
            return;
        }
        BigDecimal normalizado = preco.stripTrailingZeros();
        if (normalizado.scale() > 2 || preco.compareTo(PRECO_MAXIMO_FILTRO) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O preço deve possuir no máximo 2 casas decimais e 13 inteiros."
            );
        }
    }

    @DeleteMapping("/produto/{codigoSantri}")
    public ResponseEntity<String> deleteProduto(@PathVariable String codigoSantri) {
        exclusaoProdutoService.excluir(codigoSantri);
        return ResponseEntity.ok("Produto deletado com sucesso!");
    }

    @PutMapping(
            value = "/produto/{codigoSantri}/apresentacao",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ProdutoResponseDTO> updateApresentacao(
            @PathVariable String codigoSantri,
            @RequestParam(required = false) String nomeExibidoSite,
            @RequestParam boolean exibirNoSite,
            @RequestParam boolean destaqueNaHome,
            @RequestParam(required = false) MultipartFile imagem,
            @RequestParam(required = false) MultipartFile imagemHover,
            @jakarta.validation.Valid @org.springframework.web.bind.annotation.ModelAttribute
            com.ecommerceproject.dubaimagazinesalvador.domain.produto.PrecosPersonalizadosRequestDTO precos
    ) {
        return ResponseEntity.ok(
                apresentacaoProdutoService.atualizar(
                        codigoSantri,
                        nomeExibidoSite,
                        exibirNoSite,
                        destaqueNaHome,
                        imagem,
                        imagemHover,
                        precos
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
