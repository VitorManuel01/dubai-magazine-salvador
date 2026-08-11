package com.ecommerceproject.dubaimagazinesalvador.services.vitrine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.ecommerceproject.dubaimagazinesalvador.domain.vitrine.DepoimentoHome;
import com.ecommerceproject.dubaimagazinesalvador.domain.vitrine.DepoimentoHomeRequestDTO;
import com.ecommerceproject.dubaimagazinesalvador.repositories.DepoimentoHomeRepository;

@ExtendWith(MockitoExtension.class)
class DepoimentoHomeServiceTest {

    @Mock
    private DepoimentoHomeRepository repository;

    private DepoimentoHomeService service;

    @BeforeEach
    void setUp() {
        service = new DepoimentoHomeService(repository);
    }

    @Test
    void deveListarDepoimentosNaOrdemPersistida() {
        when(repository.findAllByOrderByPosicaoAsc()).thenReturn(depoimentosAtuais());

        var resposta = service.listar();

        assertThat(resposta).extracting("posicao").containsExactly(1, 2, 3);
    }

    @Test
    void deveAtualizarOsTresDepoimentosRemovendoEspacosExternos() {
        List<DepoimentoHome> atuais = depoimentosAtuais();
        when(repository.findAllByOrderByPosicaoAsc()).thenReturn(atuais);
        when(repository.saveAllAndFlush(atuais)).thenReturn(atuais);

        var resposta = service.atualizar(List.of(
                new DepoimentoHomeRequestDTO(1, " Ana ", " Excelente atendimento. "),
                new DepoimentoHomeRequestDTO(2, "Bruno", "Muita variedade."),
                new DepoimentoHomeRequestDTO(3, "Carla", "Equipe prestativa.")
        ));

        assertThat(resposta).extracting("nome").containsExactly("Ana", "Bruno", "Carla");
        assertThat(resposta.getFirst().texto()).isEqualTo("Excelente atendimento.");
    }

    @Test
    void deveRejeitarPosicoesRepetidasOuIncompletas() {
        var invalidos = List.of(
                new DepoimentoHomeRequestDTO(1, "Ana", "Primeiro"),
                new DepoimentoHomeRequestDTO(1, "Bruno", "Repetido"),
                new DepoimentoHomeRequestDTO(2, "Carla", "Segundo")
        );

        assertThatThrownBy(() -> service.atualizar(invalidos))
                .isInstanceOf(ResponseStatusException.class);
        verifyNoMoreInteractions(repository);
    }

    private List<DepoimentoHome> depoimentosAtuais() {
        return List.of(
                new DepoimentoHome(1, "Cliente 1", "Texto 1"),
                new DepoimentoHome(2, "Cliente 2", "Texto 2"),
                new DepoimentoHome(3, "Cliente 3", "Texto 3")
        );
    }
}
