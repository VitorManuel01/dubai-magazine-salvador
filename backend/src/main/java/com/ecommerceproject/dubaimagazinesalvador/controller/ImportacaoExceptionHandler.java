package com.ecommerceproject.dubaimagazinesalvador.controller;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import com.ecommerceproject.dubaimagazinesalvador.services.importacao.ImportacaoEmAndamentoException;
import com.ecommerceproject.dubaimagazinesalvador.services.importacao.ImportacaoOdsException;

@RestControllerAdvice
public class ImportacaoExceptionHandler {

    @ExceptionHandler(ImportacaoEmAndamentoException.class)
    public ResponseEntity<ErroImportacaoDTO> importacaoEmAndamento(
            ImportacaoEmAndamentoException exception
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ErroImportacaoDTO(exception.getMessage(), LocalDateTime.now())
        );
    }

    @ExceptionHandler(ImportacaoOdsException.class)
    public ResponseEntity<ErroImportacaoDTO> arquivoInvalido(ImportacaoOdsException exception) {
        return ResponseEntity.badRequest().body(
                new ErroImportacaoDTO(exception.getMessage(), LocalDateTime.now())
        );
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErroImportacaoDTO> arquivoMuitoGrande() {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(
                new ErroImportacaoDTO("O arquivo excede o limite de 20 MB.", LocalDateTime.now())
        );
    }

    public record ErroImportacaoDTO(String erro, LocalDateTime dataHora) {
    }
}
