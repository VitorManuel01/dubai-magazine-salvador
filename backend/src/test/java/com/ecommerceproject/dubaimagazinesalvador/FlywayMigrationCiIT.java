package com.ecommerceproject.dubaimagazinesalvador;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = DubaiMagazineSalvadorApplication.class)
@ActiveProfiles("migration-ci")
@EnabledIfEnvironmentVariable(named = "RUN_FLYWAY_MIGRATION_TEST", matches = "(?i)true")
class FlywayMigrationCiIT {

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private com.ecommerceproject.dubaimagazinesalvador.services.AuditoriaAdministrativaService auditoria;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    @Test
    void timeoutConfiguradoInterrompeConsultaLentaNoMysql() {
        long inicio = System.nanoTime();
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                entityManager.createNativeQuery("SELECT SLEEP(10)").getSingleResult())
                .isInstanceOf(jakarta.persistence.PersistenceException.class);
        assertThat(java.time.Duration.ofNanos(System.nanoTime() - inicio))
                .isLessThan(java.time.Duration.ofSeconds(8));
    }

    @Test
    void aplicaTodasAsMigrationsEmBancoVazioEValidaOSchema() {
        assertThat(flyway.info().applied()).isNotEmpty();
        assertThat(flyway.info().pending()).isEmpty();
        assertThat(flyway.info().current()).isNotNull();

        Integer produtos = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_name = 'produtos'
                """,
                Integer.class
        );
        assertThat(produtos).isEqualTo(1);
    }

    @Test
    void persisteAuditoriaSemArmazenarIpOuNavegadorEmClaro() {
        auditoria.registrar(null, "TESTE_CI", null, "PATCH", "/admin/teste", 200,
                "192.0.2.123", "Navegador de teste");
        var registro = jdbcTemplate.queryForMap("""
                SELECT ip_hmac, user_agent_hmac, ocorrido_em FROM auditoria_administrativa
                WHERE codigo_santri = ? ORDER BY id DESC LIMIT 1
                """, "TESTE_CI");
        assertThat(registro.get("ip_hmac").toString()).matches("[0-9a-f]{64}");
        assertThat(registro.get("user_agent_hmac").toString()).matches("[0-9a-f]{64}");
        assertThat(registro.get("ocorrido_em")).isNotNull();
    }
}
