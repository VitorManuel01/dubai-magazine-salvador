package com.ecommerceproject.dubaimagazinesalvador.infra.security;

import java.time.Instant;
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

    @Value("${api.Security.token.secret}")
    private String secret;

    @Value("${api.security.token.expiration-seconds:3600}")
    private long expirationSeconds;

    
    
    public String generateToken(Usuario usuario){
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            
            String token = JWT.create()
                                .withIssuer("auth-api")
                                .withSubject(usuario.getId().toString())
                                .withClaim("funcao", usuario.getFuncao().toString())
                                .withExpiresAt(generateExpirationData())
                                .sign(algorithm);

            return token;
        } catch (JWTCreationException e) {
            throw new RuntimeException("Erro ao gerar Token", e);
        }
    }

    public UUID validateToken(String token){
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            token = token.trim();
            String subject = JWT.require(algorithm)
                    .withIssuer("auth-api")
                    .build()
                    .verify(token)
                    .getSubject();
            return UUID.fromString(subject);
        } catch (JWTVerificationException | IllegalArgumentException e) {
            return null;
        }
    }

    private Instant generateExpirationData(){
        return Instant.now().plusSeconds(expirationSeconds);
    }
}
