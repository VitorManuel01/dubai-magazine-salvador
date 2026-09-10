package com.ecommerceproject.dubaimagazinesalvador.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.ConstraintViolationException;

/** Não registra a exceção: mensagens de binding podem conter senhas e outros valores recebidos. */
@RestControllerAdvice
public class ValidacaoExceptionHandler {

    @ExceptionHandler({MethodArgumentNotValidException.class,
            HandlerMethodValidationException.class, ConstraintViolationException.class,
            HttpMessageNotReadableException.class})
    public ResponseEntity<ErroValidacao> dadosInvalidos() {
        return ResponseEntity.badRequest().body(new ErroValidacao(
                "Dados inválidos. Confira os campos e o formato da solicitação."
        ));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErroValidacao> erroHttp(ResponseStatusException exception) {
        // ResponseEntity preserva o status sem sendError/redispatch e não expõe detalhes internos.
        String mensagem = switch (exception.getStatusCode().value()) {
            case 400 -> "Dados inválidos. Confira os campos e o formato da solicitação.";
            case 401 -> "Autenticação necessária.";
            case 403 -> "Acesso negado.";
            case 404 -> "Recurso não encontrado.";
            case 409 -> "A solicitação conflita com o estado atual dos dados.";
            case 413 -> "O arquivo excede o limite permitido.";
            case 429 -> "Muitas solicitações. Aguarde antes de tentar novamente.";
            case 503 -> "Serviço temporariamente indisponível. Tente novamente em instantes.";
            default -> "Não foi possível concluir a solicitação.";
        };
        return ResponseEntity.status(exception.getStatusCode())
                .headers(exception.getHeaders())
                .body(new ErroValidacao(mensagem));
    }

    public record ErroValidacao(String erro) {
    }
}
