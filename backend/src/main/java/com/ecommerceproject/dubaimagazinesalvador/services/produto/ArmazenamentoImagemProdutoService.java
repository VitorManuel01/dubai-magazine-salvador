package com.ecommerceproject.dubaimagazinesalvador.services.produto;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ArmazenamentoImagemProdutoService {

    private static final long TAMANHO_MAXIMO = 5L * 1024L * 1024L;
    private static final Map<String, String> EXTENSOES_PERMITIDAS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp"
    );

    private final Path diretorio;

    public ArmazenamentoImagemProdutoService(
            @Value("${app.upload.produtos-dir:uploads/produtos}") String diretorio
    ) {
        this.diretorio = Path.of(diretorio).toAbsolutePath().normalize();
    }

    public String salvar(String codigoSantri, MultipartFile imagem) {
        validar(imagem);
        String extensao = EXTENSOES_PERMITIDAS.get(
                imagem.getContentType().toLowerCase(Locale.ROOT)
        );
        String codigoSeguro = codigoSantri.replaceAll("[^a-zA-Z0-9_-]", "_");
        String nomeArquivo = codigoSeguro + "-" + UUID.randomUUID() + extensao;
        Path destino = diretorio.resolve(nomeArquivo).normalize();

        if (!destino.startsWith(diretorio)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Nome de arquivo de imagem inválido."
            );
        }

        try {
            Files.createDirectories(diretorio);
            try (InputStream input = imagem.getInputStream()) {
                Files.copy(input, destino);
            }
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Não foi possível armazenar a imagem.",
                    e
            );
        }

        return "/uploads/produtos/" + nomeArquivo;
    }

    private void validar(MultipartFile imagem) {
        if (imagem == null || imagem.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A imagem está vazia.");
        }
        if (imagem.getSize() > TAMANHO_MAXIMO) {
            throw new ResponseStatusException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "A imagem deve possuir no máximo 5 MB."
            );
        }
        String contentType = imagem.getContentType();
        if (contentType == null
                || !EXTENSOES_PERMITIDAS.containsKey(contentType.toLowerCase(Locale.ROOT))) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Formato não permitido. Use JPG, PNG ou WEBP."
            );
        }
    }
}
