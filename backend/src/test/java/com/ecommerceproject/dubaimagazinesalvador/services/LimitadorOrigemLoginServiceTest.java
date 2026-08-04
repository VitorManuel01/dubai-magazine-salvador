package com.ecommerceproject.dubaimagazinesalvador.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LimitadorOrigemLoginServiceTest {

    private LimitadorOrigemLoginService service;

    @BeforeEach
    void configurar() {
        service = new LimitadorOrigemLoginService(
                3,
                3,
                Duration.ofMinutes(20),
                Duration.ofMinutes(20),
                Duration.ofSeconds(1),
                Duration.ofSeconds(30),
                100
        );
    }

    @Test
    void aplicaAtrasoProgressivoDepoisDaPrimeiraFalha() {
        String dispositivo = UUID.randomUUID().toString();

        assertFalse(service.reservarTentativa("192.0.2.10", dispositivo).limitado());
        var limite = service.reservarTentativa("192.0.2.10", dispositivo);

        assertTrue(limite.limitado());
        assertTrue(limite.tentarNovamenteEm().isAfter(Instant.now()));
    }

    @Test
    void bloqueiaIpDepoisDoLimiteMesmoTrocandoDispositivo() {
        LimitadorOrigemLoginService semAtraso = novoLimitadorSemAtraso();
        semAtraso.reservarTentativa("192.0.2.20", UUID.randomUUID().toString());
        semAtraso.reservarTentativa("192.0.2.20", UUID.randomUUID().toString());
        semAtraso.reservarTentativa("192.0.2.20", UUID.randomUUID().toString());
        var limite = semAtraso.reservarTentativa(
                "192.0.2.20", UUID.randomUUID().toString()
        );

        assertTrue(limite.limitado());
        assertTrue(limite.tentarNovamenteEm().isAfter(
                Instant.now().plus(Duration.ofMinutes(19))
        ));
    }

    @Test
    void bloqueiaDispositivoMesmoQuandoOIpMuda() {
        LimitadorOrigemLoginService semAtraso = novoLimitadorSemAtraso();
        String dispositivo = UUID.randomUUID().toString();
        semAtraso.reservarTentativa("192.0.2.31", dispositivo);
        semAtraso.reservarTentativa("192.0.2.32", dispositivo);
        semAtraso.reservarTentativa("192.0.2.33", dispositivo);

        var limite = semAtraso.reservarTentativa("192.0.2.34", dispositivo);

        assertTrue(limite.limitado());
        assertTrue(limite.tentarNovamenteEm().isAfter(
                Instant.now().plus(Duration.ofMinutes(19))
        ));
    }

    @Test
    void sucessoLimpaHistoricoDoDispositivo() {
        String dispositivo = UUID.randomUUID().toString();
        service.reservarTentativa("192.0.2.40", dispositivo);

        service.registrarSucesso("192.0.2.40", dispositivo);

        assertFalse(service.reservarTentativa("192.0.2.40", dispositivo).limitado());
    }

    @Test
    void bloqueioNaoExpiraApenasPorqueAJanelaDeContagemTerminou() {
        LimitadorOrigemLoginService janelaCurta = new LimitadorOrigemLoginService(
                1,
                1,
                Duration.ZERO,
                Duration.ofMinutes(20),
                Duration.ofSeconds(1),
                Duration.ofSeconds(30),
                100
        );
        String dispositivo = UUID.randomUUID().toString();

        janelaCurta.reservarTentativa("192.0.2.50", dispositivo);

        assertTrue(janelaCurta.reservarTentativa("192.0.2.50", dispositivo).limitado());
    }

    private LimitadorOrigemLoginService novoLimitadorSemAtraso() {
        return new LimitadorOrigemLoginService(
                3,
                3,
                Duration.ofMinutes(20),
                Duration.ofMinutes(20),
                Duration.ZERO,
                Duration.ZERO,
                100
        );
    }
}
