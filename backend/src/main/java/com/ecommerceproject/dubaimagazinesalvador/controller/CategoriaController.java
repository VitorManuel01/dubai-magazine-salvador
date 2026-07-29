package com.ecommerceproject.dubaimagazinesalvador.controller;

import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.ecommerceproject.dubaimagazinesalvador.domain.categoria.Categoria;
import com.ecommerceproject.dubaimagazinesalvador.domain.categoria.CategoriaResponseDTO;
import com.ecommerceproject.dubaimagazinesalvador.repositories.CategoriaRepository;

@RestController
@RequestMapping("categoria")
public class CategoriaController {

    private static final Set<String> CATEGORIAS_EXCLUSIVAS_ADMIN = Set.of("123", "999");

    private final CategoriaRepository categoriaRepository;

    public CategoriaController(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<CategoriaResponseDTO> getAll(
            @RequestParam(defaultValue = "true") boolean somenteVisiveis,
            @RequestParam(required = false) Integer nivel,
            @RequestParam(required = false) String categoriaPaiCodigo,
            Authentication authentication
    ) {
        if (nivel != null && (nivel < 1 || nivel > 4)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O nível da categoria deve estar entre 1 e 4."
            );
        }

        List<Categoria> categorias = buscarCategorias(
                somenteVisiveis,
                nivel,
                categoriaPaiCodigo
        );

        return categorias.stream()
                .filter(categoria -> ehAdministrador(authentication)
                        || !ehCategoriaExclusivaAdmin(categoria.getCodigo()))
                .map(CategoriaResponseDTO::new)
                .toList();
    }

    private boolean ehAdministrador(Authentication authentication) {
        return authentication != null
                && authentication.getAuthorities().stream()
                        .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    private boolean ehCategoriaExclusivaAdmin(String codigo) {
        return CATEGORIAS_EXCLUSIVAS_ADMIN.stream()
                .anyMatch(codigoRaiz -> codigoRaiz.equals(codigo)
                        || codigo.startsWith(codigoRaiz + "."));
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
