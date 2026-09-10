package com.ecommerceproject.dubaimagazinesalvador.services;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Limite local em memória para impedir que rajadas atinjam as consultas e o disco.
 * O proxy continua sendo a primeira camada; este serviço é a contenção no processo.
 */
@Service
public class LimitadorRequisicoesPublicasService {

    private static final long INTERVALO_LIMPEZA = 1_024;

    private final boolean habilitado;
    private final int capacidadeCatalogo;
    private final double recargaCatalogoPorSegundo;
    private final int capacidadeImagens;
    private final double recargaImagensPorSegundo;
    private final int maximoOrigens;
    private final long inatividadeNanos;
    private final ConcurrentHashMap<ChaveLimite, Balde> baldes = new ConcurrentHashMap<>();
    private final AtomicLong requisicoes = new AtomicLong();
    private final Object travaCriacaoBalde = new Object();

    public LimitadorRequisicoesPublicasService(
            @Value("${app.security.public-rate-limit.enabled:true}") boolean habilitado,
            @Value("${app.security.public-rate-limit.catalog.capacity:60}") int capacidadeCatalogo,
            @Value("${app.security.public-rate-limit.catalog.refill-per-second:1}")
                    double recargaCatalogoPorSegundo,
            @Value("${app.security.public-rate-limit.images.capacity:240}") int capacidadeImagens,
            @Value("${app.security.public-rate-limit.images.refill-per-second:4}")
                    double recargaImagensPorSegundo,
            @Value("${app.security.public-rate-limit.max-tracked-origins:50000}")
                    int maximoOrigens,
            @Value("${app.security.public-rate-limit.entry-ttl:PT20M}") Duration inatividade
    ) {
        this.habilitado = habilitado;
        this.capacidadeCatalogo = validarInteiroPositivo(
                capacidadeCatalogo,
                "capacidade do catálogo"
        );
        this.recargaCatalogoPorSegundo = validarTaxa(
                recargaCatalogoPorSegundo,
                "recarga do catálogo"
        );
        this.capacidadeImagens = validarInteiroPositivo(
                capacidadeImagens,
                "capacidade de imagens"
        );
        this.recargaImagensPorSegundo = validarTaxa(
                recargaImagensPorSegundo,
                "recarga de imagens"
        );
        this.maximoOrigens = validarInteiroPositivo(maximoOrigens, "máximo de origens");
        if (inatividade == null || inatividade.isNegative() || inatividade.isZero()) {
            throw new IllegalArgumentException("O TTL do rate limit deve ser positivo.");
        }
        this.inatividadeNanos = inatividade.toNanos();
    }

    public Resultado consumir(String enderecoIp, TipoRequisicao tipo) {
        if (!habilitado) {
            return Resultado.aceito();
        }

        Objects.requireNonNull(tipo, "O tipo da requisição é obrigatório.");
        long agora = System.nanoTime();
        limparPeriodicamente(agora);
        ChaveLimite chave = new ChaveLimite(normalizarOrigem(enderecoIp), tipo);
        Balde balde = baldes.get(chave);
        if (balde == null) {
            synchronized (travaCriacaoBalde) {
                balde = baldes.get(chave);
                if (balde == null) {
                    if (baldes.size() >= maximoOrigens) {
                        return Resultado.bloqueado(60);
                    }
                    ConfiguracaoBalde configuracao = configuracao(tipo);
                    balde = new Balde(
                            configuracao.capacidade(),
                            configuracao.recargaPorSegundo(),
                            agora
                    );
                    baldes.put(chave, balde);
                }
            }
        }
        return balde.consumir(agora);
    }

    private void limparPeriodicamente(long agora) {
        if (requisicoes.incrementAndGet() % INTERVALO_LIMPEZA == 0) {
            limparInativos(agora);
        }
    }

    private void limparInativos(long agora) {
        baldes.entrySet().removeIf(
                entrada -> agora - entrada.getValue().ultimoAcessoNanos() > inatividadeNanos
        );
    }

    private ConfiguracaoBalde configuracao(TipoRequisicao tipo) {
        return tipo == TipoRequisicao.IMAGEM
                ? new ConfiguracaoBalde(capacidadeImagens, recargaImagensPorSegundo)
                : new ConfiguracaoBalde(capacidadeCatalogo, recargaCatalogoPorSegundo);
    }

    private String normalizarOrigem(String enderecoIp) {
        if (enderecoIp == null || enderecoIp.isBlank()) {
            return "origem-desconhecida";
        }
        String valor = enderecoIp.trim();
        return valor.length() <= 64 ? valor : valor.substring(0, 64);
    }

    private int validarInteiroPositivo(int valor, String nome) {
        if (valor < 1) {
            throw new IllegalArgumentException("A configuração de " + nome + " deve ser positiva.");
        }
        return valor;
    }

    private double validarTaxa(double valor, String nome) {
        if (!Double.isFinite(valor) || valor <= 0) {
            throw new IllegalArgumentException("A configuração de " + nome + " deve ser positiva.");
        }
        return valor;
    }

    public enum TipoRequisicao {
        CATALOGO,
        IMAGEM
    }

    public record Resultado(boolean permitido, long tentarNovamenteEmSegundos) {

        public static Resultado aceito() {
            return new Resultado(true, 0);
        }

        public static Resultado bloqueado(long segundos) {
            return new Resultado(false, Math.max(1, segundos));
        }
    }

    private record ChaveLimite(String origem, TipoRequisicao tipo) {
    }

    private record ConfiguracaoBalde(int capacidade, double recargaPorSegundo) {
    }

    private static final class Balde {

        private final int capacidade;
        private final double recargaPorSegundo;
        private double fichas;
        private long ultimaRecargaNanos;
        private volatile long ultimoAcessoNanos;

        private Balde(int capacidade, double recargaPorSegundo, long agora) {
            this.capacidade = capacidade;
            this.recargaPorSegundo = recargaPorSegundo;
            this.fichas = capacidade;
            this.ultimaRecargaNanos = agora;
            this.ultimoAcessoNanos = agora;
        }

        private synchronized Resultado consumir(long agora) {
            double segundosDecorridos = Math.max(0, agora - ultimaRecargaNanos) / 1_000_000_000d;
            fichas = Math.min(capacidade, fichas + segundosDecorridos * recargaPorSegundo);
            ultimaRecargaNanos = agora;
            ultimoAcessoNanos = agora;
            if (fichas >= 1d) {
                fichas -= 1d;
                return Resultado.aceito();
            }
            long espera = (long) Math.ceil((1d - fichas) / recargaPorSegundo);
            return Resultado.bloqueado(espera);
        }

        private long ultimoAcessoNanos() {
            return ultimoAcessoNanos;
        }
    }
}
