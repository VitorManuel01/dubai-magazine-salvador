package com.ecommerceproject.dubaimagazinesalvador.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;

import com.ecommerceproject.dubaimagazinesalvador.services.LimitadorRequisicoesPublicasService.TipoRequisicao;

class LimitadorRequisicoesPublicasServiceTest {

    @Test
    void deveBloquearQuandoAOrigemEsgotaOBalde() {
        var limitador = limitador(true, 2, 0.1, 4, 1, 100);

        assertThat(limitador.consumir("192.0.2.10", TipoRequisicao.CATALOGO).permitido())
                .isTrue();
        assertThat(limitador.consumir("192.0.2.10", TipoRequisicao.CATALOGO).permitido())
                .isTrue();

        var bloqueado = limitador.consumir("192.0.2.10", TipoRequisicao.CATALOGO);
        assertThat(bloqueado.permitido()).isFalse();
        assertThat(bloqueado.tentarNovamenteEmSegundos()).isBetween(1L, 10L);
    }

    @Test
    void deveManterBaldesIndependentesPorOrigemETipo() {
        var limitador = limitador(true, 1, 0.1, 1, 0.1, 100);

        assertThat(limitador.consumir("192.0.2.20", TipoRequisicao.CATALOGO).permitido())
                .isTrue();
        assertThat(limitador.consumir("192.0.2.20", TipoRequisicao.IMAGEM).permitido())
                .isTrue();
        assertThat(limitador.consumir("192.0.2.21", TipoRequisicao.CATALOGO).permitido())
                .isTrue();
        assertThat(limitador.consumir("192.0.2.20", TipoRequisicao.CATALOGO).permitido())
                .isFalse();
    }

    @Test
    void deveAceitarSempreQuandoDesabilitado() {
        var limitador = limitador(false, 1, 0.1, 1, 0.1, 1);

        for (int tentativa = 0; tentativa < 10; tentativa++) {
            assertThat(limitador.consumir(null, TipoRequisicao.CATALOGO).permitido()).isTrue();
        }
    }

    @Test
    void deveFalharFechadoAoAtingirOTetoDeEntradas() {
        var limitador = limitador(true, 1, 1, 1, 1, 1);

        assertThat(limitador.consumir("192.0.2.30", TipoRequisicao.CATALOGO).permitido())
                .isTrue();
        var excedente = limitador.consumir("192.0.2.31", TipoRequisicao.CATALOGO);

        assertThat(excedente.permitido()).isFalse();
        assertThat(excedente.tentarNovamenteEmSegundos()).isEqualTo(60);
    }

    @Test
    void deveRespeitarCapacidadeMesmoComConsumoConcorrente() throws Exception {
        int capacidade = 5;
        var limitador = limitador(true, capacidade, 0.01, 1, 1, 100);
        List<Callable<Boolean>> consumos = new ArrayList<>();
        for (int indice = 0; indice < 50; indice++) {
            consumos.add(() -> limitador
                    .consumir("192.0.2.40", TipoRequisicao.CATALOGO)
                    .permitido());
        }

        try (var executor = Executors.newFixedThreadPool(10)) {
            long permitidos = executor.invokeAll(consumos).stream()
                    .filter(resultado -> {
                        try {
                            return resultado.get();
                        } catch (Exception e) {
                            throw new AssertionError(e);
                        }
                    })
                    .count();

            assertThat(permitidos).isEqualTo(capacidade);
        }
    }

    @Test
    void deveRejeitarConfiguracaoInvalidaETipoAusente() {
        assertThatThrownBy(() -> limitador(true, 0, 1, 1, 1, 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> limitador(true, 1, Double.NaN, 1, 1, 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LimitadorRequisicoesPublicasService(
                true, 1, 1, 1, 1, 10, Duration.ZERO
        )).isInstanceOf(IllegalArgumentException.class);

        var limitador = limitador(true, 1, 1, 1, 1, 10);
        assertThatThrownBy(() -> limitador.consumir("192.0.2.50", null))
                .isInstanceOf(NullPointerException.class);
    }

    private LimitadorRequisicoesPublicasService limitador(
            boolean habilitado,
            int capacidadeCatalogo,
            double recargaCatalogo,
            int capacidadeImagens,
            double recargaImagens,
            int maximoOrigens
    ) {
        return new LimitadorRequisicoesPublicasService(
                habilitado,
                capacidadeCatalogo,
                recargaCatalogo,
                capacidadeImagens,
                recargaImagens,
                maximoOrigens,
                Duration.ofMinutes(20)
        );
    }
}
