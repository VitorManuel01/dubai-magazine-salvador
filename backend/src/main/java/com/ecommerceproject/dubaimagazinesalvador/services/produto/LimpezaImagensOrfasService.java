package com.ecommerceproject.dubaimagazinesalvador.services.produto;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class LimpezaImagensOrfasService {

    private static final Logger LOG = LoggerFactory.getLogger(LimpezaImagensOrfasService.class);
    private static final String CONSULTA_REFERENCIAS = """
            SELECT imagem_url FROM imagens_produto WHERE imagem_url IS NOT NULL
            UNION
            SELECT imagem_url FROM produtos WHERE imagem_url IS NOT NULL
            UNION
            SELECT imagem_hover_url FROM produtos WHERE imagem_hover_url IS NOT NULL
            UNION
            SELECT imagem_url FROM banners_home WHERE imagem_url IS NOT NULL
            UNION
            SELECT imagem_url FROM imagens_produto_vitrine_loja WHERE imagem_url IS NOT NULL
            """;

    private final JdbcTemplate jdbcTemplate;
    private final ArmazenamentoImagemProdutoService armazenamento;
    private final boolean habilitada;
    private final Duration idadeMinima;

    public LimpezaImagensOrfasService(
            JdbcTemplate jdbcTemplate,
            ArmazenamentoImagemProdutoService armazenamento,
            @Value("${app.upload.cleanup.enabled:true}") boolean habilitada,
            @Value("${app.upload.cleanup.minimum-age:PT24H}") Duration idadeMinima
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.armazenamento = armazenamento;
        this.habilitada = habilitada;
        this.idadeMinima = idadeMinima;
    }

    @Scheduled(cron = "${app.upload.cleanup.cron:0 15 3 * * *}")
    public void executar() {
        if (!habilitada) {
            return;
        }
        try {
            Set<String> nomesReferenciados = new HashSet<>();
            jdbcTemplate.queryForList(CONSULTA_REFERENCIAS, String.class).stream()
                    .map(armazenamento::nomeGerenciado)
                    .filter(java.util.Objects::nonNull)
                    .forEach(nomesReferenciados::add);
            int removidas = armazenamento.removerOrfas(nomesReferenciados, idadeMinima);
            if (removidas > 0) {
                LOG.info("Limpeza de imagens removeu {} arquivo(s) órfão(s).", removidas);
            }
        } catch (RuntimeException e) {
            LOG.error("Falha na limpeza programada de imagens órfãs.", e);
        }
    }
}
