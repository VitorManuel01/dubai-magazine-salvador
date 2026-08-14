package com.ecommerceproject.dubaimagazinesalvador.services.produto;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import com.ecommerceproject.dubaimagazinesalvador.domain.produto.Produto;
import com.ecommerceproject.dubaimagazinesalvador.repositories.ProdutoRepository;

@Service
public class ExclusaoProdutoService {

    private final ProdutoRepository produtoRepository;
    private final ArmazenamentoImagemProdutoService armazenamentoImagem;

    public ExclusaoProdutoService(
            ProdutoRepository produtoRepository,
            ArmazenamentoImagemProdutoService armazenamentoImagem
    ) {
        this.produtoRepository = produtoRepository;
        this.armazenamentoImagem = armazenamentoImagem;
    }

    @Transactional
    public void excluir(String codigoSantri) {
        Produto produto = produtoRepository.findById(codigoSantri)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Produto não encontrado: " + codigoSantri
                ));

        String imagemPrincipal = produto.getImagemUrl();
        String imagemHover = produto.getImagemHoverUrl();
        produtoRepository.delete(produto);
        produtoRepository.flush();

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            removerImagens(imagemPrincipal, imagemHover);
                        }
                    }
            );
        } else {
            removerImagens(imagemPrincipal, imagemHover);
        }
    }

    @Transactional
    public int excluirTodos(List<String> codigosInformados) {
        Set<String> codigos = normalizarCodigos(codigosInformados);
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

        List<ImagensProduto> imagens = produtos.stream()
                .map(produto -> new ImagensProduto(
                        produto.getImagemUrl(),
                        produto.getImagemHoverUrl()
                ))
                .toList();
        produtoRepository.deleteAll(produtos);
        produtoRepository.flush();
        agendarRemocaoImagens(imagens);
        return produtos.size();
    }

    private void removerImagens(String imagemPrincipal, String imagemHover) {
        armazenamentoImagem.removerSeGerenciada(imagemPrincipal);
        armazenamentoImagem.removerSeGerenciada(imagemHover);
    }

    private void agendarRemocaoImagens(List<ImagensProduto> imagens) {
        Runnable remover = () -> imagens.forEach(imagem -> removerImagens(
                imagem.principal(),
                imagem.hover()
        ));
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            remover.run();
                        }
                    }
            );
        } else {
            remover.run();
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

    private record ImagensProduto(String principal, String hover) {
    }
}
