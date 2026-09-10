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
    private static final Pattern ID_PUBLICO = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}"
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
        Produto produto = produtoRepository.findDetalhePublicoByIdPublico(
                        normalizarIdPublico(idPublico)
                )
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
            ProdutoResponseDTO resposta = new ProdutoResponseDTO(
                    produtoRepository.saveAndFlush(produto)
            );
            removerSeRollback(url);
            return resposta;
        } catch (IllegalArgumentException | IllegalStateException e) {
            armazenamentoImagem.removerSeGerenciada(url);
            throw erro(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (RuntimeException e) {
            armazenamentoImagem.removerSeGerenciada(url);
            throw e;
        }
    }

    private void removerSeRollback(String url) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status != TransactionSynchronization.STATUS_COMMITTED) {
                            armazenamentoImagem.removerSeGerenciada(url);
                        }
                    }
                }
        );
    }

    @Transactional
    public ProdutoResponseDTO removerImagem(String codigoSantri, Long imagemId) {
        Produto produto = buscarPorCodigo(codigoSantri);
        try {
            produto.removerImagem(imagemId);
        } catch (IllegalArgumentException e) {
            throw erro(HttpStatus.NOT_FOUND, e.getMessage());
        }
        ProdutoResponseDTO resposta = new ProdutoResponseDTO(
                produtoRepository.saveAndFlush(produto)
        );
        // A foto física só é removida pela limpeza órfã após conferir as demais referências.
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
        return produtoRepository.findByIdPublico(normalizarIdPublico(idPublico))
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Produto não encontrado."));
    }

    private String normalizarIdPublico(String idPublico) {
        if (idPublico == null || !ID_PUBLICO.matcher(idPublico).matches()) {
            throw erro(HttpStatus.NOT_FOUND, "Produto não encontrado.");
        }
        return idPublico.toLowerCase(java.util.Locale.ROOT);
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

    private ResponseStatusException erro(HttpStatus status, String mensagem) {
        return new ResponseStatusException(status, mensagem);
    }
}
