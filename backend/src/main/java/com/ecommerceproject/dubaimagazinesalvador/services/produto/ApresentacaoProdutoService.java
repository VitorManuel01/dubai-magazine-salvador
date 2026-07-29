package com.ecommerceproject.dubaimagazinesalvador.services.produto;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.ecommerceproject.dubaimagazinesalvador.domain.produto.Produto;
import com.ecommerceproject.dubaimagazinesalvador.domain.produto.ProdutoResponseDTO;
import com.ecommerceproject.dubaimagazinesalvador.repositories.ProdutoRepository;

@Service
public class ApresentacaoProdutoService {

    private final ProdutoRepository produtoRepository;
    private final ArmazenamentoImagemProdutoService armazenamentoImagem;

    public ApresentacaoProdutoService(
            ProdutoRepository produtoRepository,
            ArmazenamentoImagemProdutoService armazenamentoImagem
    ) {
        this.produtoRepository = produtoRepository;
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
        Produto produto = produtoRepository.findById(codigoSantri)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Produto não encontrado: " + codigoSantri
                ));

        String nomeNormalizado = normalizarNomeExibidoSite(nomeExibidoSite, produto);
        String novaImagemUrl = imagem == null || imagem.isEmpty()
                ? null
                : armazenamentoImagem.salvar(codigoSantri, imagem);
        produto.atualizarApresentacao(
                nomeNormalizado,
                exibirNoSite,
                destaqueNaHome,
                novaImagemUrl
        );

        return new ProdutoResponseDTO(produto);
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
        return nomeNormalizado.isBlank() ? produto.getDescricao() : nomeNormalizado;
    }
}
