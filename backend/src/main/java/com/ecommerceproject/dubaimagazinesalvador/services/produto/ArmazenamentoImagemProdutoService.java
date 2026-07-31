package com.ecommerceproject.dubaimagazinesalvador.services.produto;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ArmazenamentoImagemProdutoService {

    private static final long TAMANHO_MAXIMO = 5L * 1024L * 1024L;
    private static final String ARQUIVO_PUBLICO_REGEX =
            "(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png|webp)";

    private final Path diretorio;

    public ArmazenamentoImagemProdutoService(
            @Value("${app.upload.produtos-dir:uploads/produtos}") String diretorio
    ) {
        this.diretorio = Path.of(diretorio).toAbsolutePath().normalize();
    }

    public String salvar(MultipartFile imagem) {
        TipoImagem tipo = validar(imagem);
        String nomeArquivo = UUID.randomUUID() + tipo.extensaoArmazenada;
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

    public Resource carregarParaCatalogo(String nomeArquivo) {
        if (nomeArquivo == null || !nomeArquivo.matches(ARQUIVO_PUBLICO_REGEX)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Imagem não encontrada.");
        }

        try (var arquivos = Files.list(diretorio)) {
            Path arquivo = arquivos
                    .filter(Files::isRegularFile)
                    .filter(candidato -> {
                        String nomeCandidato = candidato.getFileName().toString();
                        return nomeCandidato.equalsIgnoreCase(nomeArquivo)
                                || nomeCandidato.toLowerCase(Locale.ROOT)
                                        .endsWith("-" + nomeArquivo.toLowerCase(Locale.ROOT));
                    })
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Imagem não encontrada."
                    ));
            return new FileSystemResource(arquivo);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Imagem não encontrada.",
                    e
            );
        }
    }

    private TipoImagem validar(MultipartFile imagem) {
        if (imagem == null || imagem.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A imagem está vazia.");
        }
        if (imagem.getSize() > TAMANHO_MAXIMO) {
            throw new ResponseStatusException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "A imagem deve possuir no máximo 5 MB."
            );
        }

        TipoImagem tipoReal = identificarAssinatura(imagem);
        String nomeOriginal = imagem.getOriginalFilename();
        String extensaoOriginal = extensao(nomeOriginal);
        if (extensaoOriginal == null || !tipoReal.extensoesAceitas.contains(extensaoOriginal)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A extensão da imagem não corresponde ao conteúdo do arquivo."
            );
        }

        String contentType = imagem.getContentType();
        if (contentType == null || !tipoReal.mimeType.equals(
                contentType.toLowerCase(Locale.ROOT)
        )) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O tipo declarado da imagem não corresponde ao seu conteúdo."
            );
        }
        return tipoReal;
    }

    private TipoImagem identificarAssinatura(MultipartFile imagem) {
        try (InputStream input = imagem.getInputStream()) {
            byte[] bytes = input.readNBytes(12);
            if (corresponde(bytes, 0, 0xFF, 0xD8, 0xFF)) {
                return TipoImagem.JPEG;
            }
            if (corresponde(bytes, 0, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) {
                return TipoImagem.PNG;
            }
            if (corresponde(bytes, 0, 'R', 'I', 'F', 'F')
                    && corresponde(bytes, 8, 'W', 'E', 'B', 'P')) {
                return TipoImagem.WEBP;
            }
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Não foi possível validar a imagem.",
                    e
            );
        }
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Conteúdo de imagem inválido. Use JPG, PNG ou WEBP."
        );
    }

    private boolean corresponde(byte[] bytes, int inicio, int... assinatura) {
        if (bytes.length < inicio + assinatura.length) {
            return false;
        }
        for (int indice = 0; indice < assinatura.length; indice++) {
            if (Byte.toUnsignedInt(bytes[inicio + indice]) != assinatura[indice]) {
                return false;
            }
        }
        return true;
    }

    private String extensao(String nomeArquivo) {
        if (nomeArquivo == null || nomeArquivo.length() > 255 || nomeArquivo.indexOf('\0') >= 0) {
            return null;
        }
        int separador = nomeArquivo.lastIndexOf('.');
        return separador < 0
                ? null
                : nomeArquivo.substring(separador).toLowerCase(Locale.ROOT);
    }

    private enum TipoImagem {
        JPEG("image/jpeg", ".jpg", Set.of(".jpg", ".jpeg")),
        PNG("image/png", ".png", Set.of(".png")),
        WEBP("image/webp", ".webp", Set.of(".webp"));

        private final String mimeType;
        private final String extensaoArmazenada;
        private final Set<String> extensoesAceitas;

        TipoImagem(
                String mimeType,
                String extensaoArmazenada,
                Set<String> extensoesAceitas
        ) {
            this.mimeType = mimeType;
            this.extensaoArmazenada = extensaoArmazenada;
            this.extensoesAceitas = extensoesAceitas;
        }
    }
}
