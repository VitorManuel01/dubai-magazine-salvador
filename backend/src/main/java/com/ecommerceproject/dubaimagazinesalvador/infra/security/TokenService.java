package com.ecommerceproject.dubaimagazinesalvador.infra.security;

import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.ecommerceproject.dubaimagazinesalvador.domain.usuarios.Usuario;


    

@Service
public class TokenService {

    private static final int TAMANHO_MINIMO_SEGREDO_BYTES = 32;
    private static final long EXPIRACAO_MINIMA_SEGUNDOS = 300;
    private static final long EXPIRACAO_MAXIMA_SEGUNDOS = 3_600;

    private final String secret;
    private final long expirationSeconds;
    private final String issuer;
    private final String audience;

    public TokenService(
            @Value("${api.Security.token.secret}") String secret,
            @Value("${api.security.token.expiration-seconds:3600}") long expirationSeconds,
            @Value("${api.security.token.issuer:dubai-magazine-api}") String issuer,
            @Value("${api.security.token.audience:dubai-magazine-interno}") String audience
    ) {
        validarConfiguracao(secret, expirationSeconds, issuer, audience);
        this.secret = secret;
        this.expirationSeconds = expirationSeconds;
        this.issuer = issuer;
        this.audience = audience;
    }

    
    
    public String generateToken(Usuario usuario){
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            
            Instant agora = Instant.now();
            String token = JWT.create()
                                .withIssuer(issuer)
                                .withAudience(audience)
                                .withSubject(usuario.getId().toString())
                                .withClaim("funcao", usuario.getFuncao().toString())
                                .withClaim("versao", usuario.getVersaoToken())
                                .withIssuedAt(agora)
                                .withJWTId(UUID.randomUUID().toString())
                                .withExpiresAt(agora.plusSeconds(expirationSeconds))
                                .sign(algorithm);

            return token;
        } catch (JWTCreationException e) {
            throw new RuntimeException("Erro ao gerar Token", e);
        }
    }

    public UUID validateToken(String token){
        TokenValidado validado = validateTokenCompleto(token);
        return validado == null ? null : validado.usuarioId();
    }

    public TokenValidado validateTokenCompleto(String token) {
        if (token == null || token.isBlank() || token.length() > 4096) {
            return null;
        }
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            token = token.trim();
            var jwt = JWT.require(algorithm)
                    .withIssuer(issuer)
                    .withAudience(audience)
                    .build()
                    .verify(token);
            Integer versao = jwt.getClaim("versao").asInt();
            Instant emitidoEm = jwt.getIssuedAtAsInstant();
            Instant expiraEm = jwt.getExpiresAtAsInstant();
            // A biblioteca valida datas presentes, mas não exige esses campos.
            // Ausência de exp nunca deve produzir uma sessão sem prazo.
            if (versao == null || versao < 0
                    || emitidoEm == null || expiraEm == null
                    || !expiraEm.isAfter(emitidoEm)
                    || expiraEm.isAfter(emitidoEm.plusSeconds(EXPIRACAO_MAXIMA_SEGUNDOS))
                    || jwt.getSubject() == null
                    || jwt.getId() == null || jwt.getId().isBlank()) {
                return null;
            }
            return new TokenValidado(UUID.fromString(jwt.getSubject()), versao);
        } catch (JWTVerificationException | IllegalArgumentException e) {
            return null;
        }
    }

    private void validarConfiguracao(
            String segredo,
            long expiracao,
            String emissor,
            String publico
    ) {
        if (segredo == null
                || segredo.getBytes(StandardCharsets.UTF_8).length < TAMANHO_MINIMO_SEGREDO_BYTES
                || parecePlaceholder(segredo)) {
            throw new IllegalStateException(
                    "JWT_SECRET deve ser aleatório, não previsível e possuir pelo menos 32 bytes."
            );
        }
        if (expiracao < EXPIRACAO_MINIMA_SEGUNDOS || expiracao > EXPIRACAO_MAXIMA_SEGUNDOS) {
            throw new IllegalStateException(
                    "JWT_EXPIRATION_SECONDS deve estar entre 300 e 3600 segundos."
            );
        }
        if (emissor == null || emissor.isBlank() || publico == null || publico.isBlank()) {
            throw new IllegalStateException("Emissor e audiência do JWT são obrigatórios.");
        }
    }

    private boolean parecePlaceholder(String segredo) {
        String normalizado = segredo.toLowerCase(Locale.ROOT);
        return normalizado.contains("my-secret")
                || normalizado.contains("seu-segredo")
                || normalizado.contains("defina_um_segredo")
                || normalizado.contains("change-me")
                || normalizado.contains("placeholder");
    }

    public record TokenValidado(UUID usuarioId, int versao) {
    }
}
