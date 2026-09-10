package com.ecommerceproject.dubaimagazinesalvador.services;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LimitadorOrigemLoginService {

    public static final String CABECALHO_DISPOSITIVO = "X-Device-Id";
    private static final Duration INTERVALO_LIMPEZA = Duration.ofMinutes(1);
    private static final Duration ESPERA_QUANDO_CAPACIDADE_ESGOTADA =
            Duration.ofMinutes(1);

    private final Map<ChaveOrigem, EstadoOrigem> origens = new HashMap<>();
    private final int maximoTentativasIp;
    private final int maximoTentativasDispositivo;
    private final Duration janela;
    private final Duration duracaoBloqueio;
    private final Duration atrasoInicial;
    private final Duration atrasoMaximo;
    private final int maximoOrigensRastreadas;
    private Instant proximaLimpeza = Instant.EPOCH;

    public LimitadorOrigemLoginService(
            @Value("${app.security.login.ip.max-failed-attempts:20}") int maximoTentativasIp,
            @Value("${app.security.login.device.max-failed-attempts:5}") int maximoTentativasDispositivo,
            @Value("${app.security.login.origin-window:PT20M}") Duration janela,
            @Value("${app.security.login.origin-block-duration:PT20M}") Duration duracaoBloqueio,
            @Value("${app.security.login.initial-delay:PT1S}") Duration atrasoInicial,
            @Value("${app.security.login.max-delay:PT30S}") Duration atrasoMaximo,
            @Value("${app.security.login.max-tracked-origins:50000}") int maximoOrigensRastreadas
    ) {
        if (maximoTentativasIp < 1
                || maximoTentativasDispositivo < 1
                || maximoOrigensRastreadas < 1) {
            throw new IllegalArgumentException(
                    "Os limites de login devem ser inteiros positivos."
            );
        }
        if (janela == null
                || janela.isNegative()
                || duracaoBloqueio == null
                || duracaoBloqueio.isNegative()
                || duracaoBloqueio.isZero()
                || atrasoInicial == null
                || atrasoInicial.isNegative()
                || atrasoMaximo == null
                || atrasoMaximo.isNegative()
                || atrasoInicial.compareTo(atrasoMaximo) > 0) {
            throw new IllegalArgumentException("As durações de proteção do login são inválidas.");
        }
        this.maximoTentativasIp = maximoTentativasIp;
        this.maximoTentativasDispositivo = maximoTentativasDispositivo;
        this.janela = janela;
        this.duracaoBloqueio = duracaoBloqueio;
        this.atrasoInicial = atrasoInicial;
        this.atrasoMaximo = atrasoMaximo;
        this.maximoOrigensRastreadas = maximoOrigensRastreadas;
    }

    public synchronized EstadoLimite reservarTentativa(String enderecoIp, String dispositivo) {
        Instant agora = Instant.now();
        limparExpiradosPeriodicamente(agora);
        List<ChaveOrigem> chaves = chaves(enderecoIp, dispositivo);
        Instant proximaTentativa = null;

        for (ChaveOrigem chave : chaves) {
            EstadoOrigem estado = origens.get(chave);
            if (estado == null) {
                continue;
            }
            normalizarJanela(estado, agora);
            Instant limite = maisTarde(estado.bloqueadoAte, estado.proximaTentativaEm);
            if (limite != null && agora.isBefore(limite)) {
                proximaTentativa = maisTarde(proximaTentativa, limite);
            }
        }

        if (proximaTentativa != null) {
            return EstadoLimite.limitado(proximaTentativa);
        }

        for (ChaveOrigem chave : chaves) {
            EstadoOrigem estado = obterOuCriar(chave, agora);
            if (estado == null) {
                if (chave.tipo == TipoOrigem.IP) {
                    return EstadoLimite.limitado(
                            agora.plus(ESPERA_QUANDO_CAPACIDADE_ESGOTADA)
                    );
                }
                continue;
            }
            normalizarJanela(estado, agora);
            estado.tentativas++;

            int maximo = chave.tipo == TipoOrigem.IP
                    ? maximoTentativasIp
                    : maximoTentativasDispositivo;
            if (estado.tentativas >= maximo) {
                estado.bloqueadoAte = agora.plus(duracaoBloqueio);
                estado.proximaTentativaEm = estado.bloqueadoAte;
            } else {
                Duration atraso = calcularAtraso(estado.tentativas);
                estado.proximaTentativaEm = agora.plus(atraso);
            }
        }

        return EstadoLimite.liberado();
    }

    public synchronized void registrarSucesso(String enderecoIp, String dispositivo) {
        origens.remove(new ChaveOrigem(TipoOrigem.IP, normalizarIp(enderecoIp)));
        String dispositivoNormalizado = normalizarDispositivo(dispositivo);
        if (dispositivoNormalizado != null) {
            origens.remove(new ChaveOrigem(TipoOrigem.DISPOSITIVO, dispositivoNormalizado));
        }
    }

    private List<ChaveOrigem> chaves(String enderecoIp, String dispositivo) {
        List<ChaveOrigem> chaves = new ArrayList<>(2);
        chaves.add(new ChaveOrigem(TipoOrigem.IP, normalizarIp(enderecoIp)));
        String dispositivoNormalizado = normalizarDispositivo(dispositivo);
        if (dispositivoNormalizado != null) {
            chaves.add(new ChaveOrigem(TipoOrigem.DISPOSITIVO, dispositivoNormalizado));
        }
        return chaves;
    }

    private EstadoOrigem obterOuCriar(ChaveOrigem chave, Instant agora) {
        EstadoOrigem existente = origens.get(chave);
        if (existente != null) {
            return existente;
        }
        if (origens.size() >= maximoOrigensRastreadas) {
            return null;
        }
        EstadoOrigem novo = new EstadoOrigem(agora);
        origens.put(chave, novo);
        return novo;
    }

    private void normalizarJanela(EstadoOrigem estado, Instant agora) {
        if (estado.bloqueadoAte != null && agora.isBefore(estado.bloqueadoAte)) {
            return;
        }
        if (!agora.isBefore(estado.inicioJanela.plus(janela))) {
            estado.reiniciar(agora);
        }
    }

    private Duration calcularAtraso(int tentativas) {
        long multiplicador = 1L << Math.min(Math.max(tentativas - 1, 0), 20);
        Duration calculado;
        try {
            calculado = atrasoInicial.multipliedBy(multiplicador);
        } catch (ArithmeticException e) {
            calculado = atrasoMaximo;
        }
        return calculado.compareTo(atrasoMaximo) > 0 ? atrasoMaximo : calculado;
    }

    private void limparExpirados(Instant agora) {
        origens.entrySet().removeIf(entry -> {
            EstadoOrigem estado = entry.getValue();
            Instant expiraEm = maisTarde(
                    estado.inicioJanela.plus(janela),
                    estado.bloqueadoAte
            );
            return expiraEm != null && !agora.isBefore(expiraEm);
        });
    }

    private void limparExpiradosPeriodicamente(Instant agora) {
        if (agora.isBefore(proximaLimpeza)) {
            return;
        }
        limparExpirados(agora);
        proximaLimpeza = agora.plus(INTERVALO_LIMPEZA);
    }

    private String normalizarIp(String enderecoIp) {
        if (enderecoIp == null || enderecoIp.isBlank() || enderecoIp.length() > 64) {
            return "desconhecido";
        }
        return enderecoIp.trim();
    }

    private String normalizarDispositivo(String dispositivo) {
        if (dispositivo == null || dispositivo.isBlank() || dispositivo.length() > 64) {
            return null;
        }
        try {
            return UUID.fromString(dispositivo.trim()).toString();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Instant maisTarde(Instant primeiro, Instant segundo) {
        if (primeiro == null) {
            return segundo;
        }
        if (segundo == null) {
            return primeiro;
        }
        return primeiro.isAfter(segundo) ? primeiro : segundo;
    }

    private enum TipoOrigem {
        IP,
        DISPOSITIVO
    }

    private record ChaveOrigem(TipoOrigem tipo, String valor) {
    }

    private static final class EstadoOrigem {
        private int tentativas;
        private Instant inicioJanela;
        private Instant proximaTentativaEm;
        private Instant bloqueadoAte;

        private EstadoOrigem(Instant agora) {
            reiniciar(agora);
        }

        private void reiniciar(Instant agora) {
            tentativas = 0;
            inicioJanela = agora;
            proximaTentativaEm = null;
            bloqueadoAte = null;
        }
    }

    public record EstadoLimite(boolean limitado, Instant tentarNovamenteEm) {

        private static EstadoLimite liberado() {
            return new EstadoLimite(false, null);
        }

        private static EstadoLimite limitado(Instant tentarNovamenteEm) {
            return new EstadoLimite(true, tentarNovamenteEm);
        }
    }
}
