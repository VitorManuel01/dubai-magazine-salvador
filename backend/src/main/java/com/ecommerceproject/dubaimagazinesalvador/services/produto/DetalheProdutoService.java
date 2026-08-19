package com.ecommerceproject.dubaimagazinesalvador.services.produto;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.ecommerceproject.dubaimagazinesalvador.domain.produto.Produto;
import com.ecommerceproject.dubaimagazinesalvador.domain.produto.ProdutoDetalhePublicoDTO;
import com.ecommerceproject.dubaimagazinesalvador.domain.produto.ProdutoResponseDTO;
import com.ecommerceproject.dubaimagazinesalvador.repositories.ProdutoRepository;

@Service
public class DetalheProdutoService {

    private static final int LIMITE_DESCRICAO = 20_000;
    private static final Set<Integer> TAMANHOS_PERMITIDOS = Set.of(
            12, 14, 16, 18, 20, 24, 28, 32
    );
    private static final Pattern ABERTURA_TAMANHO = Pattern.compile(
            "\\[tamanho=(\\d{1,2})]"
    );

    private final ProdutoRepository produtoRepository;
    private final ArmazenamentoImagemProdutoService armazenamentoImagem;

    public DetalheProdutoService(
            ProdutoRepository produtoRepository,
            ArmazenamentoImagemProdutoService armazenamentoImagem
    ) {
        this.produtoRepository = produtoRepository;
        this.armazenamentoImagem = armazenamentoImagem;
    }

    @Transactional(readOnly = true)
    public ProdutoDetalhePublicoDTO buscarPublico(String idPublico) {
        Produto produto = produtoRepository.findDetalhePublicoByIdPublico(idPublico)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Produto não encontrado no catálogo."
                ));
        return new ProdutoDetalhePublicoDTO(produto);
    }

    @Transactional(readOnly = true)
    public ProdutoResponseDTO buscarAdministracao(String idPublico) {
        return new ProdutoResponseDTO(buscarPorIdPublico(idPublico));
    }

    @Transactional
    public ProdutoResponseDTO atualizarDescricao(String codigoSantri, String descricao) {
        Produto produto = buscarPorCodigo(codigoSantri);
        produto.atualizarDescricaoSite(normalizarDescricao(descricao));
        return new ProdutoResponseDTO(produtoRepository.saveAndFlush(produto));
    }

    @Transactional
    public ProdutoResponseDTO adicionarImagem(String codigoSantri, MultipartFile arquivo) {
        Produto produto = buscarPorCodigo(codigoSantri);
        if (produto.getImagens().size() >= 8) {
            throw erro(HttpStatus.CONFLICT, "O produto já possui o limite de 8 fotos.");
        }

        String url = armazenamentoImagem.salvar(arquivo);
        try {
            produto.adicionarImagem(url);
            return new ProdutoResponseDTO(produtoRepository.saveAndFlush(produto));
        } catch (IllegalArgumentException | IllegalStateException e) {
            armazenamentoImagem.removerSeGerenciada(url);
            throw erro(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @Transactional
    public ProdutoResponseDTO removerImagem(String codigoSantri, Long imagemId) {
        Produto produto = buscarPorCodigo(codigoSantri);
        String url;
        try {
            url = produto.removerImagem(imagemId);
        } catch (IllegalArgumentException e) {
            throw erro(HttpStatus.NOT_FOUND, e.getMessage());
        }
        ProdutoResponseDTO resposta = new ProdutoResponseDTO(
                produtoRepository.saveAndFlush(produto)
        );
        removerDepoisDoCommit(url);
        return resposta;
    }

    @Transactional
    public ProdutoResponseDTO reordenarImagens(
            String codigoSantri,
            List<Long> imagens
    ) {
        Produto produto = buscarPorCodigo(codigoSantri);
        try {
            produto.reordenarImagens(imagens);
        } catch (IllegalArgumentException e) {
            throw erro(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        return new ProdutoResponseDTO(produtoRepository.saveAndFlush(produto));
    }

    private Produto buscarPorCodigo(String codigoSantri) {
        return produtoRepository.findById(codigoSantri)
                .orElseThrow(() -> erro(
                        HttpStatus.NOT_FOUND,
                        "Produto não encontrado: " + codigoSantri
                ));
    }

    private Produto buscarPorIdPublico(String idPublico) {
        return produtoRepository.findByIdPublico(idPublico)
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Produto não encontrado."));
    }

    private String normalizarDescricao(String descricao) {
        if (descricao == null || descricao.isBlank()) {
            return null;
        }
        String normalizada = descricao
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replace("\t", "    ")
                .trim();
        if (normalizada.length() > LIMITE_DESCRICAO) {
            throw erro(
                    HttpStatus.BAD_REQUEST,
                    "A descrição deve possuir no máximo 20.000 caracteres."
            );
        }
        if (normalizada.indexOf('\0') >= 0) {
            throw erro(HttpStatus.BAD_REQUEST, "A descrição contém caracteres inválidos.");
        }

        Matcher aberturas = ABERTURA_TAMANHO.matcher(normalizada);
        int quantidadeAberturas = 0;
        while (aberturas.find()) {
            quantidadeAberturas++;
            int tamanho = Integer.parseInt(aberturas.group(1));
            if (!TAMANHOS_PERMITIDOS.contains(tamanho)) {
                throw erro(
                        HttpStatus.BAD_REQUEST,
                        "A descrição contém um tamanho de fonte não permitido."
                );
            }
        }
        int quantidadeFechamentos = contarOcorrencias(normalizada, "[/tamanho]");
        if (quantidadeAberturas != quantidadeFechamentos) {
            throw erro(
                    HttpStatus.BAD_REQUEST,
                    "A formatação de tamanho da descrição está incompleta."
            );
        }
        return normalizada;
    }

    private int contarOcorrencias(String texto, String trecho) {
        int total = 0;
        int inicio = 0;
        while ((inicio = texto.indexOf(trecho, inicio)) >= 0) {
            total++;
            inicio += trecho.length();
        }
        return total;
    }

    private void removerDepoisDoCommit(String url) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            armazenamentoImagem.removerSeGerenciada(url);
                        }
                    }
            );
            return;
        }
        armazenamentoImagem.removerSeGerenciada(url);
    }

    private ResponseStatusException erro(HttpStatus status, String mensagem) {
        return new ResponseStatusException(status, mensagem);
    }
}
