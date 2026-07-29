package com.ecommerceproject.dubaimagazinesalvador.services.vitrine;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ecommerceproject.dubaimagazinesalvador.domain.categoria.Categoria;
import com.ecommerceproject.dubaimagazinesalvador.domain.produto.ProdutoResponseDTO;
import com.ecommerceproject.dubaimagazinesalvador.domain.vitrine.VitrineHome;
import com.ecommerceproject.dubaimagazinesalvador.domain.vitrine.VitrineHomeRequestDTO;
import com.ecommerceproject.dubaimagazinesalvador.domain.vitrine.VitrineHomeResponseDTO;
import com.ecommerceproject.dubaimagazinesalvador.repositories.CategoriaRepository;
import com.ecommerceproject.dubaimagazinesalvador.repositories.ProdutoRepository;
import com.ecommerceproject.dubaimagazinesalvador.repositories.VitrineHomeRepository;

@Service
public class VitrineHomeService {

    private static final int PRODUTOS_POR_VITRINE = 4;

    private final VitrineHomeRepository vitrineRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProdutoRepository produtoRepository;

    public VitrineHomeService(
            VitrineHomeRepository vitrineRepository,
            CategoriaRepository categoriaRepository,
            ProdutoRepository produtoRepository
    ) {
        this.vitrineRepository = vitrineRepository;
        this.categoriaRepository = categoriaRepository;
        this.produtoRepository = produtoRepository;
    }

    @Transactional(readOnly = true)
    public List<VitrineHomeResponseDTO> listarPublicas() {
        return vitrineRepository.findByAtivoTrueOrderByOrdemAscIdAsc().stream()
                .map(this::montarResposta)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VitrineHomeResponseDTO> listarAdministracao() {
        return vitrineRepository.findAllByOrderByOrdemAscIdAsc().stream()
                .map(this::montarResposta)
                .toList();
    }

    @Transactional
    public VitrineHomeResponseDTO criar(VitrineHomeRequestDTO dados) {
        DadosNormalizados normalizados = validar(dados);
        if (vitrineRepository.existsByCategoria_Codigo(normalizados.categoria().getCodigo())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Já existe uma vitrine para esta categoria."
            );
        }
        VitrineHome vitrine = vitrineRepository.save(new VitrineHome(
                normalizados.categoria(),
                normalizados.titulo(),
                normalizados.descricao(),
                normalizados.ordem(),
                dados.ativo()
        ));
        return montarResposta(vitrine);
    }

    @Transactional
    public VitrineHomeResponseDTO atualizar(Long id, VitrineHomeRequestDTO dados) {
        VitrineHome vitrine = buscar(id);
        DadosNormalizados normalizados = validar(dados);
        if (vitrineRepository.existsByCategoria_CodigoAndIdNot(
                normalizados.categoria().getCodigo(),
                id
        )) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Já existe uma vitrine para esta categoria."
            );
        }
        vitrine.atualizar(
                normalizados.categoria(),
                normalizados.titulo(),
                normalizados.descricao(),
                normalizados.ordem(),
                dados.ativo()
        );
        return montarResposta(vitrine);
    }

    @Transactional
    public void excluir(Long id) {
        vitrineRepository.delete(buscar(id));
    }

    private VitrineHome buscar(Long id) {
        return vitrineRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Vitrine não encontrada."
                ));
    }

    private DadosNormalizados validar(VitrineHomeRequestDTO dados) {
        if (dados == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe os dados da vitrine.");
        }
        String categoriaCodigo = normalizarObrigatorio(
                dados.categoriaCodigo(),
                "A categoria é obrigatória."
        );
        String titulo = normalizarObrigatorio(dados.titulo(), "O título é obrigatório.");
        String descricao = normalizarObrigatorio(
                dados.descricao(),
                "A descrição é obrigatória."
        );
        int ordem = dados.ordem() == null ? 0 : dados.ordem();

        if (titulo.length() > 180) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O título deve possuir no máximo 180 caracteres."
            );
        }
        if (descricao.length() > 1000) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A descrição deve possuir no máximo 1000 caracteres."
            );
        }
        if (ordem < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A ordem não pode ser negativa."
            );
        }
        Categoria categoria = categoriaRepository.findById(categoriaCodigo)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Categoria não encontrada: " + categoriaCodigo
                ));
        return new DadosNormalizados(categoria, titulo, descricao, ordem);
    }

    private String normalizarObrigatorio(String valor, String mensagem) {
        if (valor == null || valor.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, mensagem);
        }
        return valor.trim();
    }

    private VitrineHomeResponseDTO montarResposta(VitrineHome vitrine) {
        List<ProdutoResponseDTO> produtos = produtoRepository.findCatalogoPorCategoria(
                vitrine.getCategoria().getCodigo(),
                null,
                false,
                false,
                PageRequest.of(0, PRODUTOS_POR_VITRINE)
        ).map(ProdutoResponseDTO::new).getContent();
        return new VitrineHomeResponseDTO(vitrine, produtos);
    }

    private record DadosNormalizados(
            Categoria categoria,
            String titulo,
            String descricao,
            int ordem
    ) {
    }
}
