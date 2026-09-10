package com.ecommerceproject.dubaimagazinesalvador.infra.security;

import static org.assertj.core.api.Assertions.assertThat;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.ecommerceproject.dubaimagazinesalvador.repositories.UsuarioRepository;

@ActiveProfiles("preproduction")
@SpringBootTest(classes = ErrosHttpServidorTest.AplicacaoTeste.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.config.name=regressao-http", "server.address=127.0.0.1",
        "ALLOW_LOCAL_ADMIN_REGISTRATION=true", "app.security.require-https=true",
        "server.forward-headers-strategy=native", "spring.flyway.enabled=false"
})
class CadastroAdminLocalHttpTest {
    @Value("${local.server.port}") int porta;
    @MockitoBean TokenService tokens;
    @MockitoBean UsuarioRepository usuarios;

    private int post(String... extras) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + porta + "/auth/registerADM"))
                .timeout(Duration.ofSeconds(10)).header("Content-Type", "application/json")
                .header("X-Forwarded-Proto", "https");
        for (int i = 0; i < extras.length; i += 2) builder.header(extras[i], extras[i + 1]);
        return HttpClient.newHttpClient().send(builder.POST(HttpRequest.BodyPublishers.ofString("{}"))
                .build(), HttpResponse.BodyHandlers.ofString()).statusCode();
    }

    @Test void rejeitaInsomniaSemAutenticacaoMesmoComAntigaFlagHabilitada() throws Exception {
        assertThat(post()).isEqualTo(401);
    }

    @Test void rejeitaTunelMesmoSeForwardedForInformarLoopback() throws Exception {
        assertThat(post("X-Forwarded-For", "127.0.0.1", "X-Real-IP", "127.0.0.1",
                "CF-Connecting-IP", "203.0.113.10")).isEqualTo(401);
        assertThat(post("X-Forwarded-For", "203.0.113.10")).isEqualTo(401);
    }

    @Test void rejeitaTokenInvalido() throws Exception {
        assertThat(post("Authorization", "Bearer invalido")).isEqualTo(401);
    }
}
