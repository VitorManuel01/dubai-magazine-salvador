package com.ecommerceproject.dubaimagazinesalvador.infra.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.mock.env.MockEnvironment;

class PerfilExecucaoSeguroTest {

    @Test
    void rejeitaSegredosIguaisEmProducao() {
        var ambiente = new MockEnvironment();
        ambiente.setActiveProfiles("production");
        ambiente.withProperty("api.Security.token.secret", "segredo-longo-que-nao-deve-ser-reutilizado-123456")
                .withProperty("app.security.audit.hmac-secret", "segredo-longo-que-nao-deve-ser-reutilizado-123456");

        assertThatThrownBy(() -> new PerfilExecucaoSeguro(ambiente)
                .run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("segredos distintos");
    }

    @Test
    void rejeitaPlaceholderDeAuditoriaEmPreProducao() {
        var ambiente = new MockEnvironment();
        ambiente.setActiveProfiles("preproduction");
        ambiente.withProperty("api.Security.token.secret", "segredo-jwt-aleatorio-com-mais-de-32-bytes-123456")
                .withProperty("app.security.audit.hmac-secret", "gere_outro_segredo_aleatorio_independente");

        assertThatThrownBy(() -> new PerfilExecucaoSeguro(ambiente)
                .run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("segredos distintos");
    }
}
