package com.ecommerceproject.dubaimagazinesalvador.services.vitrine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.ecommerceproject.dubaimagazinesalvador.domain.vitrine.BannerHome;
import com.ecommerceproject.dubaimagazinesalvador.repositories.BannerHomeRepository;
import com.ecommerceproject.dubaimagazinesalvador.services.produto.ArmazenamentoImagemProdutoService;

@ExtendWith(MockitoExtension.class)
class BannerHomeServiceTest {

    @Mock
    private BannerHomeRepository repository;

    @Mock
    private ArmazenamentoImagemProdutoService armazenamentoImagem;

    private BannerHomeService service;

    @BeforeEach
    void setUp() {
        service = new BannerHomeService(repository, armazenamentoImagem);
    }

    @Test
    void deveListarOsTresBannersNaOrdemPersistida() {
        when(repository.findAllByOrderByPosicaoAsc()).thenReturn(List.of(
                new BannerHome(1, "/catalogo/imagens/primeira.webp"),
                new BannerHome(2, null),
                new BannerHome(3, "/catalogo/imagens/terceira.webp")
        ));

        var banners = service.listar();

        assertThat(banners).extracting("posicao").containsExactly(1, 2, 3);
        assertThat(banners.get(1).imagemUrl()).isNull();
    }

    @Test
    void deveTrocarImagemEPreservarFotoAnteriorParaVerificacaoDeReferencias() {
        String antiga = "/catalogo/imagens/11111111-1111-4111-8111-111111111111.webp";
        String identificadorNovo = "22222222-2222-4222-8222-222222222222.webp";
        BannerHome banner = new BannerHome(2, antiga);
        MockMultipartFile imagem = new MockMultipartFile(
                "imagem",
                "banner.webp",
                "image/webp",
                new byte[] { 'R', 'I', 'F', 'F' }
        );

        when(repository.findById(2)).thenReturn(Optional.of(banner));
        when(armazenamentoImagem.salvar(imagem))
                .thenReturn("/uploads/produtos/" + identificadorNovo);
        when(repository.saveAndFlush(banner)).thenReturn(banner);

        var resposta = service.atualizar(2, imagem);

        assertThat(resposta.imagemUrl())
                .isEqualTo("/catalogo/imagens/" + identificadorNovo);
        verify(armazenamentoImagem, never()).removerSeGerenciada(antiga);
    }

    @Test
    void deveRejeitarPosicaoForaDosTresBanners() {
        MockMultipartFile imagem = new MockMultipartFile(
                "imagem",
                "banner.webp",
                "image/webp",
                new byte[] { 'R', 'I', 'F', 'F' }
        );

        assertThatThrownBy(() -> service.atualizar(4, imagem))
                .isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(repository, armazenamentoImagem);
    }
}
