package com.ecommerceproject.dubaimagazinesalvador.services.produto;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.ecommerceproject.dubaimagazinesalvador.domain.produto.Produto;
import com.ecommerceproject.dubaimagazinesalvador.domain.produto.ProdutoResponseDTO;
import com.ecommerceproject.dubaimagazinesalvador.domain.produto.PrecosPersonalizadosRequestDTO;
import com.ecommerceproject.dubaimagazinesalvador.repositories.ControleSelecaoHomeRepository;
import com.ecommerceproject.dubaimagazinesalvador.repositories.ProdutoRepository;

@Service
public class ApresentacaoProdutoService {

    private static final long LIMITE_DESTAQUES_HOME = 3;

    private final ProdutoRepository produtoRepository;
    private final ControleSelecaoHomeRepository controleSelecaoHomeRepository;
    private final ArmazenamentoImagemProdutoService armazenamentoImagem;

    public ApresentacaoProdutoService(
            ProdutoRepository produtoRepository,
            ControleSelecaoHomeRepository controleSelecaoHomeRepository,
            ArmazenamentoImagemProdutoService armazenamentoImagem
    ) {
        this.produtoRepository = produtoRepository;
        this.controleSelecaoHomeRepository = controleSelecaoHomeRepository;
        this.armazenamentoImagem = armazenamentoImagem;
    }

    @Transactional
    public ProdutoResponseDTO atualizar(
            String codigoSantri,
            String nomeExibidoSite,
            boolean exibirNoSite,
            boolean destaqueNaHome,
            MultipartFile imagem
    ) {
        return atualizar(
                codigoSantri,
                nomeExibidoSite,
                exibirNoSite,
                destaqueNaHome,
                imagem,
                null
        );
    }

    @Transactional
    public ProdutoResponseDTO atualizar(
            String codigoSantri,
            String nomeExibidoSite,
            boolean exibirNoSite,
            boolean destaqueNaHome,
            MultipartFile imagem,
            MultipartFile imagemHover
    ) {
        return atualizar(codigoSantri, nomeExibidoSite, exibirNoSite, destaqueNaHome, imagem, imagemHover, null);
    }

    @Transactional
    public ProdutoResponseDTO atualizar(
            String codigoSantri, String nomeExibidoSite, boolean exibirNoSite, boolean destaqueNaHome,
            MultipartFile imagem, MultipartFile imagemHover, PrecosPersonalizadosRequestDTO precos
    ) {
        controleSelecaoHomeRepository.bloquearParaAtualizacao()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "O controle da Seleção da Loja não está configurado."
                ));

        Produto produto = produtoRepository.findById(codigoSantri)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Produto não encontrado: " + codigoSantri
                ));

        if (destaqueNaHome && !exibirNoSite) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Um produto oculto não pode fazer parte da Seleção da Loja."
            );
        }

        if (destaqueNaHome && !produto.isDisponivelUltimaImportacao()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Um produto indisponível na última importação não pode fazer parte da Seleção da Loja."
            );
        }

        if (destaqueNaHome
                && !produto.isDestaqueNaHome()
                && produtoRepository.countByDestaqueNaHomeTrue() >= LIMITE_DESTAQUES_HOME) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A Seleção da Loja já possui o limite de 3 produtos."
            );
        }

        String nomeNormalizado = normalizarNomeExibidoSite(nomeExibidoSite, produto);
        if (precos != null) precos.aplicar(produto);
        String novaImagemUrl = null;
        String novaImagemHoverUrl = null;
        try {
            novaImagemUrl = imagem == null || imagem.isEmpty()
                    ? null
                    : armazenamentoImagem.salvar(imagem);
            novaImagemHoverUrl = imagemHover == null || imagemHover.isEmpty()
                    ? null
                    : armazenamentoImagem.salvar(imagemHover);
            produto.atualizarApresentacao(
                    nomeNormalizado,
                    exibirNoSite,
                    destaqueNaHome,
                    novaImagemUrl,
                    novaImagemHoverUrl
            );
            Produto salvo = produtoRepository.saveAndFlush(produto);
            registrarCicloDeVidaImagens(
                    novaImagemUrl,
                    novaImagemHoverUrl
            );
            return new ProdutoResponseDTO(salvo);
        } catch (RuntimeException e) {
            armazenamentoImagem.removerSeGerenciada(novaImagemUrl);
            armazenamentoImagem.removerSeGerenciada(novaImagemHoverUrl);
            throw e;
        }
    }

    private String normalizarNomeExibidoSite(String nomeExibidoSite, Produto produto) {
        if (nomeExibidoSite == null) {
            return produto.getNomeExibidoSite();
        }
        String nomeNormalizado = nomeExibidoSite.trim().replaceAll("\\s+", " ");
        if (nomeNormalizado.length() > 500) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O nome exibido no site deve possuir no máximo 500 caracteres."
            );
        }
        return nomeNormalizado.isBlank() ? produto.getNome() : nomeNormalizado;
    }

    private void registrarCicloDeVidaImagens(
            String novaPrincipal,
            String novaHover
    ) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status != TransactionSynchronization.STATUS_COMMITTED) {
                            armazenamentoImagem.removerSeGerenciada(novaPrincipal);
                            armazenamentoImagem.removerSeGerenciada(novaHover);
                        }
                    }
                }
        );
    }

}
