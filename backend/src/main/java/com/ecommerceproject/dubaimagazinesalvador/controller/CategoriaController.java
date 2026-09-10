package com.ecommerceproject.dubaimagazinesalvador.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.ecommerceproject.dubaimagazinesalvador.domain.categoria.Categoria;
import com.ecommerceproject.dubaimagazinesalvador.domain.categoria.CategoriaCatalogoPublicoDTO;
import com.ecommerceproject.dubaimagazinesalvador.domain.categoria.CategoriaResponseDTO;
import com.ecommerceproject.dubaimagazinesalvador.repositories.CategoriaRepository;
import com.ecommerceproject.dubaimagazinesalvador.services.produto.FiltrosPesquisaCatalogo;

@RestController
public class CategoriaController {

    private final CategoriaRepository categoriaRepository;

    public CategoriaController(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    @GetMapping("/categoria")
    @Transactional(readOnly = true)
    public List<CategoriaCatalogoPublicoDTO> getAll(
            @RequestParam(required = false) Integer nivel,
            @RequestParam(required = false) String categoriaPaiCodigo
    ) {
        validarNivel(nivel);
        return buscarCategorias(
                true,
                nivel,
                FiltrosPesquisaCatalogo.normalizarCodigoCategoria(categoriaPaiCodigo)
        ).stream()
                .filter(Categoria::podeExibirNoCatalogoPublico)
                .map(CategoriaCatalogoPublicoDTO::new)
                .toList();
    }

    @GetMapping("/admin/categorias")
    @Transactional(readOnly = true)
    public List<CategoriaResponseDTO> getAllAdministracao(
            @RequestParam(defaultValue = "false") boolean somenteVisiveis,
            @RequestParam(required = false) Integer nivel,
            @RequestParam(required = false) String categoriaPaiCodigo
    ) {
        validarNivel(nivel);
        return buscarCategorias(
                somenteVisiveis,
                nivel,
                FiltrosPesquisaCatalogo.normalizarCodigoCategoria(categoriaPaiCodigo)
        ).stream()
                .map(CategoriaResponseDTO::new)
                .toList();
    }

    private void validarNivel(Integer nivel) {
        if (nivel != null && (nivel < 1 || nivel > 4)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O nível da categoria deve estar entre 1 e 4."
            );
        }

    }

    private List<Categoria> buscarCategorias(
            boolean somenteVisiveis,
            Integer nivel,
            String categoriaPaiCodigo
    ) {
        if (categoriaPaiCodigo != null && !categoriaPaiCodigo.isBlank()) {
            String codigo = categoriaPaiCodigo.trim();
            return somenteVisiveis
                    ? categoriaRepository
                            .findByCategoriaPai_CodigoAndExibirNoSiteTrueOrderByNomeAsc(codigo)
                    : categoriaRepository.findByCategoriaPai_CodigoOrderByNomeAsc(codigo);
        }
        if (nivel == null) {
            return somenteVisiveis
                    ? categoriaRepository.findByExibirNoSiteTrueOrderByCaminhoAsc()
                    : categoriaRepository.findAll();
        }
        return somenteVisiveis
                ? categoriaRepository.findByNivelAndExibirNoSiteTrueOrderByNomeAsc(nivel)
                : categoriaRepository.findByNivelOrderByNomeAsc(nivel);
    }
}
