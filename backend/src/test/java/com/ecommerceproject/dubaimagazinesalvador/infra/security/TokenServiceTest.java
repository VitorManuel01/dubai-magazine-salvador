package com.ecommerceproject.dubaimagazinesalvador.infra.security;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.ecommerceproject.dubaimagazinesalvador.domain.usuarios.Funcionario;
import com.ecommerceproject.dubaimagazinesalvador.domain.usuarios.Role;

class TokenServiceTest {

    private static final String SEGREDO = "M8!zQ4#vL2@rT9$xP7&kN5*cD3+wF6_yH1";

    @Test
    void deveGerarJwtValidoPorUmaHora() {
        TokenService service = novoServico();

        Funcionario usuario = new Funcionario();
        usuario.setId(UUID.randomUUID());
        usuario.setCodigoSantri("FUNC-001");
        usuario.setFuncao(Role.ROLE_FUNCIONARIO);
        Instant emitidoDepoisDe = Instant.now();

        String token = service.generateToken(usuario);
        Instant expiraEm = JWT.decode(token).getExpiresAtAsInstant();
        long duracao = Duration.between(emitidoDepoisDe, expiraEm).getSeconds();

        assertTrue(duracao >= 3599 && duracao <= 3600);
        org.junit.jupiter.api.Assertions.assertEquals(
                usuario.getId(),
                service.validateToken(token)
        );
    }

    @Test
    void deveRejeitarTokenInvalido() {
        TokenService service = novoServico();

        assertNull(service.validateToken("token-invalido"));
        assertNull(service.validateToken(null));
        assertNull(service.validateToken(" "));
    }

    @Test
    void deveRejeitarJwtAssinadoSemExpiracao() {
        assertNull(novoServico().validateToken(tokenAssinado(builder ->
                builder.withIssuedAt(Instant.now()))));
    }

    @Test
    void deveRejeitarJwtAssinadoSemDataDeEmissao() {
        assertNull(novoServico().validateToken(tokenAssinado(builder ->
                builder.withExpiresAt(Instant.now().plusSeconds(3600)))));
    }

    @Test
    void deveRejeitarJwtComValidadeAcimaDeUmaHora() {
        Instant agora = Instant.now();
        assertNull(novoServico().validateToken(tokenAssinado(builder -> builder
                .withIssuedAt(agora).withExpiresAt(agora.plusSeconds(3601)))));
    }

    @Test
    void deveRejeitarJwtComEmissorOuAudienciaDiferente() {
        Instant agora = Instant.now();
        assertNull(novoServico().validateToken(tokenAssinado(builder -> builder
                .withIssuedAt(agora).withExpiresAt(agora.plusSeconds(3600))
                .withIssuer("outra-aplicacao"))));
        assertNull(novoServico().validateToken(tokenAssinado(builder -> builder
                .withIssuedAt(agora).withExpiresAt(agora.plusSeconds(3600))
                .withAudience("outro-publico"))));
    }

    private String tokenAssinado(Consumer<JWTCreator.Builder> configurar) {
        var builder = JWT.create()
                .withIssuer("dubai-magazine-api-teste")
                .withAudience("dubai-magazine-interno-teste")
                .withSubject(UUID.randomUUID().toString())
                .withClaim("versao", 0)
                .withJWTId(UUID.randomUUID().toString());
        configurar.accept(builder);
        return builder.sign(Algorithm.HMAC256(SEGREDO));
    }

    private TokenService novoServico() {
        return new TokenService(
                SEGREDO,
                3600,
                "dubai-magazine-api-teste",
                "dubai-magazine-interno-teste"
        );
    }
}
