package com.ecommerceproject.dubaimagazinesalvador.infra.config;

import java.util.Arrays;
import java.util.Set;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** Impede que um servidor seja publicado acidentalmente com regras de desenvolvimento. */
@Component
public class PerfilExecucaoSeguro implements ApplicationRunner {

    private static final Set<String> PERFIS_PERMITIDOS = Set.of(
            "development",
            "preproduction",
            "production",
            "test",
            "migration-ci"
    );

    private final Environment environment;

    public PerfilExecucaoSeguro(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        String[] ativos = environment.getActiveProfiles();
        if (ativos.length != 1 || !PERFIS_PERMITIDOS.contains(ativos[0])) {
            throw new IllegalStateException(
                    "Defina exatamente um SPRING_PROFILES_ACTIVE entre: "
                            + String.join(", ", PERFIS_PERMITIDOS)
                            + ". Perfis recebidos: " + Arrays.toString(ativos)
            );
        }
        if ("production".equals(ativos[0]) || "preproduction".equals(ativos[0])) {
            String jwt = environment.getProperty("api.Security.token.secret");
            String auditoria = environment.getProperty("app.security.audit.hmac-secret");
            if (jwt == null || auditoria == null || jwt.isBlank() || auditoria.isBlank()
                    || jwt.equals(auditoria) || parecePlaceholder(auditoria)) {
                throw new IllegalStateException(
                        "JWT_SECRET e AUDIT_HMAC_SECRET devem ser segredos distintos e reais."
                );
            }
        }
    }

    private boolean parecePlaceholder(String segredo) {
        String normalizado = segredo.toLowerCase(java.util.Locale.ROOT);
        return normalizado.contains("gere_")
                || normalizado.contains("sua_senha")
                || normalizado.contains("change-me")
                || normalizado.contains("placeholder")
                || normalizado.contains("seu-segredo");
    }
}
