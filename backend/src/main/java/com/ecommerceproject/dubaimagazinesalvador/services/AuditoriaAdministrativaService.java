package com.ecommerceproject.dubaimagazinesalvador.services;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerceproject.dubaimagazinesalvador.domain.usuarios.Role;

@Service
public class AuditoriaAdministrativaService {

    private static final String ALGORITMO_HMAC = "HmacSHA256";

    private final JdbcTemplate jdbcTemplate;
    private final byte[] segredoHmac;
    private final java.time.Duration retencao;
    private final int maximoExclusoesPorExecucao;

    public AuditoriaAdministrativaService(
            JdbcTemplate jdbcTemplate,
            @Value("${app.security.audit.hmac-secret}") String segredoHmac,
            @Value("${app.security.audit.retention:P180D}") java.time.Duration retencao,
            @Value("${app.security.audit.cleanup.max-rows:10000}") int maximoExclusoesPorExecucao
    ) {
        if (segredoHmac == null
                || segredoHmac.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "AUDIT_HMAC_SECRET deve possuir pelo menos 32 bytes."
            );
        }
        if (retencao == null || retencao.isNegative() || retencao.isZero()) {
            throw new IllegalStateException("A retenção da auditoria deve ser positiva.");
        }
        if (maximoExclusoesPorExecucao < 1 || maximoExclusoesPorExecucao > 100_000) {
            throw new IllegalStateException(
                    "O limite de limpeza da auditoria deve estar entre 1 e 100000."
            );
        }
        this.jdbcTemplate = jdbcTemplate;
        this.segredoHmac = segredoHmac.getBytes(StandardCharsets.UTF_8).clone();
        this.retencao = retencao;
        this.maximoExclusoesPorExecucao = maximoExclusoesPorExecucao;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(
            UUID usuarioId,
            String codigoSantri,
            Role funcao,
            String metodo,
            String caminho,
            int statusHttp,
            String enderecoIp,
            String userAgent
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO auditoria_administrativa (
                    usuario_id, codigo_santri, funcao, metodo, caminho,
                    status_http, ip_hmac, user_agent_hmac, ocorrido_em
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                usuarioId == null ? null : usuarioId.toString(),
                limitar(codigoSantri, 255),
                funcao == null ? null : funcao.name(),
                limitar(metodo, 10),
                limitar(caminho, 500),
                statusHttp,
                hmac(enderecoIp),
                hmac(userAgent),
                Instant.now()
        );
    }

    private String hmac(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            Mac mac = Mac.getInstance(ALGORITMO_HMAC);
            mac.init(new SecretKeySpec(segredoHmac, ALGORITMO_HMAC));
            return HexFormat.of().formatHex(
                    mac.doFinal(valor.getBytes(StandardCharsets.UTF_8))
            );
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Não foi possível registrar a auditoria.", e);
        }
    }

    private String limitar(String valor, int maximo) {
        if (valor == null) {
            return null;
        }
        return valor.length() <= maximo ? valor : valor.substring(0, maximo);
    }

    /** Mantém a trilha útil sem permitir crescimento indefinido da tabela. */
    @Scheduled(cron = "${app.security.audit.cleanup.cron:0 40 3 * * *}")
    public void removerRegistrosExpirados() {
        jdbcTemplate.update(
                """
                DELETE FROM auditoria_administrativa
                WHERE ocorrido_em < ?
                ORDER BY ocorrido_em
                LIMIT ?
                """,
                java.sql.Timestamp.from(Instant.now().minus(retencao)),
                maximoExclusoesPorExecucao
        );
    }
}
