package com.ecommerceproject.dubaimagazinesalvador.services.produto;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ecommerceproject.dubaimagazinesalvador.domain.produto.Produto;
import com.ecommerceproject.dubaimagazinesalvador.domain.produto.VisibilidadeProdutosRequestDTO;
import com.ecommerceproject.dubaimagazinesalvador.domain.produto.VisibilidadeProdutosResponseDTO;
import com.ecommerceproject.dubaimagazinesalvador.repositories.ProdutoRepository;

@Service
public class VisibilidadeProdutosService {

    private final ProdutoRepository produtoRepository;

    public VisibilidadeProdutosService(ProdutoRepository produtoRepository) {
        this.produtoRepository = produtoRepository;
    }

    @Transactional
    public VisibilidadeProdutosResponseDTO alterarVisibilidade(
            VisibilidadeProdutosRequestDTO requisicao
    ) {
        Set<String> codigos = normalizarCodigos(requisicao.codigosSantri());
        List<Produto> produtos = produtoRepository.findAllById(codigos);
        if (produtos.size() != codigos.size()) {
            Set<String> encontrados = produtos.stream()
                    .map(Produto::getCodigoSantri)
                    .collect(java.util.stream.Collectors.toSet());
            String ausente = codigos.stream()
                    .filter(codigo -> !encontrados.contains(codigo))
                    .findFirst()
                    .orElse("desconhecido");
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Produto não encontrado: " + ausente
            );
        }

        boolean exibirNoSite = Boolean.TRUE.equals(requisicao.exibirNoSite());
        if (exibirNoSite) {
            validarDisponibilidadeParaExibicao(produtos);
        }

        int alterados = 0;
        for (Produto produto : produtos) {
            if (produto.isExibirNoSite() == exibirNoSite) {
                continue;
            }
            if (exibirNoSite) {
                produto.tornarVisivelNoSite();
            } else {
                produto.ocultarNoSite();
            }
            alterados++;
        }
        return new VisibilidadeProdutosResponseDTO(codigos.size(), alterados, exibirNoSite);
    }

    private void validarDisponibilidadeParaExibicao(List<Produto> produtos) {
        List<String> indisponiveis = produtos.stream()
                .filter(produto -> !produto.isDisponivelUltimaImportacao())
                .map(Produto::getCodigoSantri)
                .toList();
        if (!indisponiveis.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Produtos indisponíveis na última importação não podem ser exibidos: "
                            + String.join(", ", indisponiveis)
            );
        }
    }

    private Set<String> normalizarCodigos(List<String> codigosInformados) {
        if (codigosInformados == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione ao menos um produto.");
        }
        Set<String> codigos = new LinkedHashSet<>();
        for (String codigo : codigosInformados) {
            if (codigo == null || codigo.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código Santri inválido.");
            }
            codigos.add(codigo.trim());
        }
        if (codigos.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione ao menos um produto.");
        }
        if (codigos.size() > 200) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Selecione no máximo 200 produtos por operação."
            );
        }
        return codigos;
    }
}
