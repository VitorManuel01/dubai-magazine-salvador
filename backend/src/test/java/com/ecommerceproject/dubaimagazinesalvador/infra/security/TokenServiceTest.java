package com.ecommerceproject.dubaimagazinesalvador.infra.security;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.auth0.jwt.JWT;
import com.ecommerceproject.dubaimagazinesalvador.domain.usuarios.Funcionario;
import com.ecommerceproject.dubaimagazinesalvador.domain.usuarios.Role;

class TokenServiceTest {

    @Test
    void deveGerarJwtValidoPorUmaHora() {
        TokenService service = new TokenService();
        ReflectionTestUtils.setField(
                service,
                "secret",
                "segredo-de-teste-com-tamanho-suficiente-para-hmac"
        );
        ReflectionTestUtils.setField(service, "expirationSeconds", 3600L);

        Funcionario usuario = new Funcionario();
        usuario.setId(UUID.randomUUID());
        usuario.setLogin("funcionario");
        usuario.setFuncao(Role.ROLE_FUNCIONARIO);
        Instant emitidoDepoisDe = Instant.now();

        String token = service.generateToken(usuario);
        Instant expiraEm = JWT.decode(token).getExpiresAtAsInstant();
        long duracao = Duration.between(emitidoDepoisDe, expiraEm).getSeconds();

        assertTrue(duracao >= 3599 && duracao <= 3600);
    }

    @Test
    void deveRejeitarTokenInvalido() {
        TokenService service = new TokenService();
        ReflectionTestUtils.setField(
                service,
                "secret",
                "segredo-de-teste-com-tamanho-suficiente-para-hmac"
        );

        assertNull(service.validateToken("token-invalido"));
    }
}
