package com.ecommerceproject.dubaimagazinesalvador.infra.security;

import java.time.Instant;

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
                                .withSubject(usuario.getLogin())
                                .withClaim("id", usuario.getId().toString())
                                .withClaim("funcao", usuario.getFuncao().toString())
                                .withExpiresAt(generateExpirationData())
                                .sign(algorithm);

            return token;
        } catch (JWTCreationException e) {
            throw new RuntimeException("Erro ao gerar Token", e);
        }
    }

    public String validateToken(String token){
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            token = token.trim();
            return JWT.require(algorithm)
                    .withIssuer("auth-api")
                    .build()
                    .verify(token)
                    .getSubject();
        } catch (JWTVerificationException e) {
            return null;
        }
    }

    private Instant generateExpirationData(){
        return Instant.now().plusSeconds(expirationSeconds);
    }
}
