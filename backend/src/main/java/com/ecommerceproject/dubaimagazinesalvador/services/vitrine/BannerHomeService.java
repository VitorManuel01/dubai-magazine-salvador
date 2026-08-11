package com.ecommerceproject.dubaimagazinesalvador.services.vitrine;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.ecommerceproject.dubaimagazinesalvador.domain.produto.ImagemProdutoCatalogo;
import com.ecommerceproject.dubaimagazinesalvador.domain.vitrine.BannerHome;
import com.ecommerceproject.dubaimagazinesalvador.domain.vitrine.BannerHomeResponseDTO;
import com.ecommerceproject.dubaimagazinesalvador.repositories.BannerHomeRepository;
import com.ecommerceproject.dubaimagazinesalvador.services.produto.ArmazenamentoImagemProdutoService;

@Service
public class BannerHomeService {

    private static final int PRIMEIRA_POSICAO = 1;
    private static final int ULTIMA_POSICAO = 3;

    private final BannerHomeRepository repository;
    private final ArmazenamentoImagemProdutoService armazenamentoImagem;

    public BannerHomeService(
            BannerHomeRepository repository,
            ArmazenamentoImagemProdutoService armazenamentoImagem
    ) {
        this.repository = repository;
        this.armazenamentoImagem = armazenamentoImagem;
    }

    public List<BannerHomeResponseDTO> listar() {
        return repository.findAllByOrderByPosicaoAsc().stream()
                .map(BannerHomeResponseDTO::new)
                .toList();
    }

    public BannerHomeResponseDTO atualizar(int posicao, MultipartFile imagem) {
        validarPosicao(posicao);
        BannerHome banner = repository.findById(posicao)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Banner não encontrado."
                ));

        String imagemAnterior = banner.getImagemUrl();
        String caminhoArmazenado = armazenamentoImagem.salvar(imagem);
        String novaImagem = ImagemProdutoCatalogo.criarUrlPublica(caminhoArmazenado);

        try {
            banner.atualizarImagem(novaImagem);
            BannerHome salvo = repository.saveAndFlush(banner);
            armazenamentoImagem.removerSeGerenciada(imagemAnterior);
            return new BannerHomeResponseDTO(salvo);
        } catch (RuntimeException e) {
            armazenamentoImagem.removerSeGerenciada(novaImagem);
            throw e;
        }
    }

    private void validarPosicao(int posicao) {
        if (posicao < PRIMEIRA_POSICAO || posicao > ULTIMA_POSICAO) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A posição do banner deve estar entre 1 e 3."
            );
        }
    }
}
