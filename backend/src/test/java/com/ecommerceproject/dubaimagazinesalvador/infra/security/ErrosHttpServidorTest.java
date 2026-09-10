package com.ecommerceproject.dubaimagazinesalvador.infra.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.ecommerceproject.dubaimagazinesalvador.controller.ValidacaoExceptionHandler;
import com.ecommerceproject.dubaimagazinesalvador.domain.usuarios.Administrador;
import com.ecommerceproject.dubaimagazinesalvador.domain.usuarios.Role;
import com.ecommerceproject.dubaimagazinesalvador.repositories.UsuarioRepository;

import jakarta.servlet.http.HttpServletResponse;

/** Usa Tomcat real: MockMvc não reproduz o despacho ERROR após sendError. Sem banco de dados. */
@SpringBootTest(classes = ErrosHttpServidorTest.AplicacaoTeste.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
                "spring.config.name=regressao-http",
                "server.address=127.0.0.1",
                "app.security.require-https=false",
                "spring.flyway.enabled=false",
                "server.error.include-message=never",
                "server.error.include-stacktrace=never",
                "server.error.include-binding-errors=never"
        })
class ErrosHttpServidorTest {
    @Value("${local.server.port}")
    private int porta;

    @MockitoBean
    private TokenService tokenService;
    @MockitoBean
    private UsuarioRepository usuarioRepository;

    private Administrador usuario;
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();

    @BeforeEach
    void autenticarUsuarioDeTeste() {
        usuario = new Administrador();
        usuario.setId(UUID.randomUUID());
        usuario.setAtivo(true);
        usuario.setFuncao(Role.ROLE_ADMIN);
        when(tokenService.validateTokenCompleto("token-teste"))
                .thenReturn(new TokenService.TokenValidado(usuario.getId(), 0));
        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));
    }

    @Test
    void paginaInvalidaPreserva400ComESemAutenticacao() throws Exception {
        assertThat(get("/produto?pagina=5001", false).statusCode()).isEqualTo(400);
        var resposta = get("/produto?pagina=5001", true);
        assertThat(resposta.statusCode()).isEqualTo(400);
        assertThat(resposta.body()).contains("Dados inválidos").doesNotContain("detalhe-interno");
        assertThat(get("/interno/produtos", true).statusCode()).isEqualTo(200);
    }

    @Test
    void recursoAusentePreserva404SemInvalidarSessao() throws Exception {
        assertThat(get("/produto/ausente", true).statusCode()).isEqualTo(404);
        assertThat(get("/produto/ausente", false).statusCode()).isEqualTo(404);
        assertThat(get("/interno/produtos", true).statusCode()).isEqualTo(200);
    }

    @Test
    void erroDoContainerPreserva500SemExporMensagemOuInvalidarSessao() throws Exception {
        for (boolean autenticado : new boolean[]{false, true}) {
            var resposta = get("/produto/erro-servlet", autenticado);
            assertThat(resposta.statusCode()).isEqualTo(500);
            assertThat(resposta.body()).doesNotContain("detalhe-interno", "trace", "Exception");
        }
        assertThat(get("/interno/produtos", true).statusCode()).isEqualTo(200);
    }

    @Test
    void naoLiberaAcessoDiretoAoErroNemRotasProtegidas() throws Exception {
        assertThat(get("/error", false).statusCode()).isEqualTo(401);
        assertThat(get("/interno/produtos", false).statusCode()).isEqualTo(401);
        assertThat(get("/admin/produtos", false).statusCode()).isEqualTo(401);
        usuario.setFuncao(Role.ROLE_FUNCIONARIO);
        assertThat(get("/admin/produtos", true).statusCode()).isEqualTo(403);
    }

    @Test
    void tokenRevogadoContinuaRetornando401() throws Exception {
        usuario.invalidarTokensEmitidos();
        assertThat(get("/interno/produtos", true).statusCode()).isEqualTo(401);
        assertThat(get("/produto?pagina=5001", true).statusCode()).isEqualTo(401);
    }

    private HttpResponse<String> get(String caminho, boolean autenticado) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + porta + caminho))
                .timeout(Duration.ofSeconds(10)).header("Accept", "application/json");
        if (autenticado) request.header("Authorization", "Bearer token-teste");
        return client.send(request.GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    @Configuration(proxyBeanMethods = false)
    @TestComponent
    @EnableAutoConfiguration(excludeName = {
            "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
            "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration"
    })
    @Import({SecurityConfigurations.class, SecurityFilter.class, HttpsObrigatorioFilter.class,
            ValidacaoExceptionHandler.class, EndpointsTeste.class})
    static class AplicacaoTeste { }

    @RestController
    @TestComponent
    static class EndpointsTeste {
        @org.springframework.web.bind.annotation.PostMapping("/auth/registerADM")
        String cadastroSimulado() { return "cadastro-simulado-sem-banco"; }

        @GetMapping("/produto")
        String catalogo(@RequestParam(defaultValue = "0") int pagina) {
            if (pagina > 5000) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "detalhe-interno");
            return "ok";
        }

        @GetMapping("/produto/ausente")
        String ausente() {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "detalhe-interno");
        }

        @GetMapping("/produto/erro-servlet")
        void falha(HttpServletResponse response) throws IOException {
            response.sendError(500, "detalhe-interno");
        }

        @GetMapping({"/interno/produtos", "/admin/produtos"})
        String protegido() { return "ok"; }
    }
}
