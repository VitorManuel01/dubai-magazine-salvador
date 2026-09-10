package com.ecommerceproject.dubaimagazinesalvador.services.produto;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private static final int DIMENSAO_MAXIMA = 12_000;
    private static final long PIXELS_MAXIMOS = 40_000_000L;
    private static final String ARQUIVO_PUBLICO_REGEX =
            "(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png|webp)";
    private static final Pattern IDENTIFICADOR_PUBLICO_NO_FINAL = Pattern.compile(
            "([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(?:jpg|png|webp))$",
            Pattern.CASE_INSENSITIVE
    );

    private final Path diretorio;
    private final Map<String, Path> indiceArquivosLegados = new ConcurrentHashMap<>();

    public ArmazenamentoImagemProdutoService(
            @Value("${app.upload.produtos-dir:uploads/produtos}") String diretorio
    ) {
        this.diretorio = Path.of(diretorio).toAbsolutePath().normalize();
        inicializarDiretorioEIndiceLegado();
    }

    public String salvar(MultipartFile imagem) {
        validarTamanhoDeclarado(imagem);
        Path temporario = null;
        try {
            validarDiretorioSeguro();
            temporario = Files.createTempFile(diretorio, ".imagem-upload-", ".tmp");
            try (InputStream input = imagem.getInputStream();
                    OutputStream output = Files.newOutputStream(
                            temporario, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS
                    )) {
                byte[] buffer = new byte[8192];
                long total = 0;
                int lidos;
                while ((lidos = input.read(buffer)) != -1) {
                    total += lidos;
                    if (total > TAMANHO_MAXIMO) {
                        throw new ResponseStatusException(
                                HttpStatus.PAYLOAD_TOO_LARGE,
                                "A imagem deve possuir no máximo 5 MB."
                        );
                    }
                    output.write(buffer, 0, lidos);
                }
            }
            // A assinatura e as dimensões são verificadas nos mesmos bytes que serão publicados.
            TipoImagem tipo = validar(imagem, temporario);
            String nomeArquivo = UUID.randomUUID() + tipo.extensaoArmazenada;
            Path destino = diretorio.resolve(nomeArquivo);
            validarDiretorioSeguro();
            try {
                Files.move(temporario, destino, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporario, destino);
            }
            return "/uploads/produtos/" + nomeArquivo;
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Não foi possível armazenar a imagem.",
                    e
            );
        } finally {
            if (temporario != null) {
                try {
                    validarDiretorioSeguro();
                    Files.deleteIfExists(temporario);
                } catch (IOException ignored) {
                    // O arquivo parcial nunca recebe um identificador publicamente acessível.
                }
            }
        }
    }

    public Resource carregarParaCatalogo(String nomeArquivo) {
        if (nomeArquivo == null || !nomeArquivo.matches(ARQUIVO_PUBLICO_REGEX)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Imagem não encontrada.");
        }

        Path arquivo = localizarArquivoGerenciado(nomeArquivo);
        if (arquivo == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Imagem não encontrada.");
        }
        return new FileSystemResource(arquivo);
    }

    public void removerSeGerenciada(String imagemUrl) {
        String nomeArquivo = extrairNomeGerenciado(imagemUrl);
        if (nomeArquivo == null) {
            return;
        }

        Path arquivo = localizarArquivoGerenciado(nomeArquivo);
        if (arquivo == null || !arquivo.startsWith(diretorio)) {
            return;
        }

        try {
            Files.deleteIfExists(arquivo);
            indiceArquivosLegados.remove(nomeArquivo.toLowerCase(Locale.ROOT), arquivo);
        } catch (IOException ignored) {
            // A imagem nova já está persistida. Uma falha de limpeza não deve
            // desfazer a troca do banner nem quebrar o catálogo.
        }
    }

    private Path localizarArquivoGerenciado(String nomeArquivo) {
        try {
            validarDiretorioSeguro();
        } catch (IOException e) {
            return null;
        }
        Path direto = diretorio.resolve(nomeArquivo).normalize();
        if (direto.startsWith(diretorio)
                && Files.isRegularFile(direto, LinkOption.NOFOLLOW_LINKS)
                && !Files.isSymbolicLink(direto)) {
            return direto;
        }

        Path legado = indiceArquivosLegados.get(nomeArquivo.toLowerCase(Locale.ROOT));
        if (legado == null
                || !legado.startsWith(diretorio)
                || Files.isSymbolicLink(legado)
                || !Files.isRegularFile(legado, LinkOption.NOFOLLOW_LINKS)) {
            if (legado != null) {
                indiceArquivosLegados.remove(nomeArquivo.toLowerCase(Locale.ROOT), legado);
            }
            return null;
        }
        return legado;
    }

    private void inicializarDiretorioEIndiceLegado() {
        try {
            validarAncestraisSemLinks();
            Files.createDirectories(diretorio);
            validarDiretorioSeguro();
            try (var arquivos = Files.list(diretorio)) {
                arquivos.filter(arquivo -> Files.isRegularFile(
                                arquivo,
                                LinkOption.NOFOLLOW_LINKS
                        ))
                        .filter(arquivo -> !Files.isSymbolicLink(arquivo))
                        .forEach(this::indexarArquivoLegado);
            }
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Não foi possível preparar o diretório persistente de imagens.",
                    e
            );
        }
    }

    private void validarAncestraisSemLinks() throws IOException {
        for (Path atual = diretorio; atual != null; atual = atual.getParent()) {
            if (Files.isSymbolicLink(atual)
                    || (Files.exists(atual, LinkOption.NOFOLLOW_LINKS)
                        && !atual.toRealPath().equals(atual))) {
                throw new IOException("O diretório de imagens não pode usar links simbólicos.");
            }
        }
    }

    private void validarDiretorioSeguro() throws IOException {
        validarAncestraisSemLinks();
        if (!Files.isDirectory(diretorio, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Diretório de imagens indisponível.");
        }
    }

    private void indexarArquivoLegado(Path arquivo) {
        String nome = arquivo.getFileName().toString();
        Matcher matcher = IDENTIFICADOR_PUBLICO_NO_FINAL.matcher(nome);
        if (matcher.find() && !nome.equalsIgnoreCase(matcher.group(1))) {
            indiceArquivosLegados.putIfAbsent(
                    matcher.group(1).toLowerCase(Locale.ROOT),
                    arquivo.normalize()
            );
        }
    }

    private String extrairNomeGerenciado(String imagemUrl) {
        if (imagemUrl == null || imagemUrl.isBlank()) {
            return null;
        }
        int separador = imagemUrl.lastIndexOf('/');
        String nomeArquivo = separador >= 0
                ? imagemUrl.substring(separador + 1)
                : imagemUrl;
        Matcher identificador = IDENTIFICADOR_PUBLICO_NO_FINAL.matcher(nomeArquivo);
        return identificador.find() ? identificador.group(1).toLowerCase(Locale.ROOT) : null;
    }

    public String nomeGerenciado(String imagemUrl) {
        return extrairNomeGerenciado(imagemUrl);
    }

    public int removerOrfas(Set<String> nomesReferenciados, Duration idadeMinima) {
        if (nomesReferenciados == null) {
            throw new IllegalArgumentException("As referências de imagens devem ter sido consultadas.");
        }
        if (idadeMinima == null || idadeMinima.isNegative() || idadeMinima.isZero()) {
            throw new IllegalArgumentException("A idade mínima para limpeza deve ser positiva.");
        }
        Set<String> referencias = nomesReferenciados.stream()
                        .filter(java.util.Objects::nonNull)
                        .map(nome -> nome.toLowerCase(Locale.ROOT))
                        .collect(java.util.stream.Collectors.toUnmodifiableSet());
        Instant limite = Instant.now().minus(idadeMinima);
        int removidas = 0;
        try {
            validarDiretorioSeguro();
            try (var arquivos = Files.list(diretorio)) {
                var iterador = arquivos.iterator();
                while (iterador.hasNext()) {
                    Path arquivo = iterador.next();
                    if (Files.isSymbolicLink(arquivo)
                            || !Files.isRegularFile(arquivo, LinkOption.NOFOLLOW_LINKS)
                            || Files.getLastModifiedTime(arquivo, LinkOption.NOFOLLOW_LINKS)
                                    .toInstant().isAfter(limite)) {
                        continue;
                    }
                    String nome = arquivo.getFileName().toString();
                    if (nome.matches("\\.imagem-upload-\\d+\\.tmp")) {
                        // Recupera temporários abandonados se o processo foi interrompido.
                        if (Files.deleteIfExists(arquivo)) {
                            removidas++;
                        }
                        continue;
                    }
                    Matcher identificador = IDENTIFICADOR_PUBLICO_NO_FINAL.matcher(nome);
                    if (!identificador.find()
                            || referencias.contains(identificador.group(1).toLowerCase(Locale.ROOT))) {
                        continue;
                    }
                    if (Files.deleteIfExists(arquivo)) {
                        indiceArquivosLegados.remove(
                                identificador.group(1).toLowerCase(Locale.ROOT),
                                arquivo
                        );
                        removidas++;
                    }
                }
            }
            return removidas;
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível limpar imagens órfãs.", e);
        }
    }

    private void validarTamanhoDeclarado(MultipartFile imagem) {
        if (imagem == null || imagem.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A imagem está vazia.");
        }
        if (imagem.getSize() > TAMANHO_MAXIMO) {
            throw new ResponseStatusException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "A imagem deve possuir no máximo 5 MB."
            );
        }
    }

    private TipoImagem validar(MultipartFile imagem, Path arquivo) {
        TipoImagem tipoReal = identificarAssinatura(arquivo);
        validarDimensoes(arquivo, tipoReal);
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

    private void validarDimensoes(Path imagem, TipoImagem tipo) {
        try (InputStream input = Files.newInputStream(imagem, LinkOption.NOFOLLOW_LINKS)) {
            Dimensoes dimensoes = switch (tipo) {
                case PNG -> lerDimensoesPng(input);
                case JPEG -> lerDimensoesJpeg(input);
                case WEBP -> lerDimensoesWebp(input);
            };
            long pixels = (long) dimensoes.largura() * dimensoes.altura();
            if (dimensoes.largura() < 1
                    || dimensoes.altura() < 1
                    || dimensoes.largura() > DIMENSAO_MAXIMA
                    || dimensoes.altura() > DIMENSAO_MAXIMA
                    || pixels > PIXELS_MAXIMOS) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "As dimensões da imagem excedem o limite seguro."
                );
            }
        } catch (ResponseStatusException e) {
            throw e;
        } catch (IOException | IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Não foi possível confirmar as dimensões da imagem.",
                    e
            );
        }
    }

    private Dimensoes lerDimensoesPng(InputStream input) throws IOException {
        byte[] cabecalho = input.readNBytes(24);
        if (cabecalho.length != 24 || !corresponde(
                cabecalho,
                12,
                'I', 'H', 'D', 'R'
        )) {
            throw new IllegalArgumentException("Cabeçalho PNG inválido.");
        }
        return new Dimensoes(
                inteiroBigEndian(cabecalho, 16),
                inteiroBigEndian(cabecalho, 20)
        );
    }

    private Dimensoes lerDimensoesJpeg(InputStream input) throws IOException {
        if (input.read() != 0xFF || input.read() != 0xD8) {
            throw new IllegalArgumentException("Cabeçalho JPEG inválido.");
        }
        while (true) {
            int prefixo;
            do {
                prefixo = input.read();
            } while (prefixo != -1 && prefixo != 0xFF);
            if (prefixo == -1) {
                break;
            }

            int marcador;
            do {
                marcador = input.read();
            } while (marcador == 0xFF);
            if (marcador == -1 || marcador == 0xDA || marcador == 0xD9) {
                break;
            }
            if (marcador == 0x00 || marcador == 0x01 || marcador == 0xD8
                    || (marcador >= 0xD0 && marcador <= 0xD7)) {
                continue;
            }

            int tamanho = lerUnsignedShort(input);
            if (tamanho < 2) {
                throw new IllegalArgumentException("Segmento JPEG inválido.");
            }
            if (ehMarcadorDimensoesJpeg(marcador)) {
                if (tamanho < 7 || input.read() == -1) {
                    throw new IllegalArgumentException("Dimensões JPEG inválidas.");
                }
                int altura = lerUnsignedShort(input);
                int largura = lerUnsignedShort(input);
                return new Dimensoes(largura, altura);
            }
            input.skipNBytes(tamanho - 2L);
        }
        throw new IllegalArgumentException("JPEG sem dimensões válidas.");
    }

    private Dimensoes lerDimensoesWebp(InputStream input) throws IOException {
        byte[] cabecalho = input.readNBytes(32);
        if (cabecalho.length < 30
                || !corresponde(cabecalho, 0, 'R', 'I', 'F', 'F')
                || !corresponde(cabecalho, 8, 'W', 'E', 'B', 'P')) {
            throw new IllegalArgumentException("Cabeçalho WEBP inválido.");
        }

        if (corresponde(cabecalho, 12, 'V', 'P', '8', 'X')) {
            return new Dimensoes(
                    inteiro24LittleEndian(cabecalho, 24) + 1,
                    inteiro24LittleEndian(cabecalho, 27) + 1
            );
        }
        if (corresponde(cabecalho, 12, 'V', 'P', '8', 'L')
                && Byte.toUnsignedInt(cabecalho[20]) == 0x2F) {
            int bits = inteiroLittleEndian(cabecalho, 21);
            return new Dimensoes(
                    (bits & 0x3FFF) + 1,
                    ((bits >>> 14) & 0x3FFF) + 1
            );
        }
        if (corresponde(cabecalho, 12, 'V', 'P', '8', ' ')
                && corresponde(cabecalho, 23, 0x9D, 0x01, 0x2A)) {
            int largura = (Byte.toUnsignedInt(cabecalho[26])
                    | Byte.toUnsignedInt(cabecalho[27]) << 8) & 0x3FFF;
            int altura = (Byte.toUnsignedInt(cabecalho[28])
                    | Byte.toUnsignedInt(cabecalho[29]) << 8) & 0x3FFF;
            return new Dimensoes(largura, altura);
        }
        throw new IllegalArgumentException("Formato interno WEBP inválido.");
    }

    private int inteiroBigEndian(byte[] bytes, int inicio) {
        return Byte.toUnsignedInt(bytes[inicio]) << 24
                | Byte.toUnsignedInt(bytes[inicio + 1]) << 16
                | Byte.toUnsignedInt(bytes[inicio + 2]) << 8
                | Byte.toUnsignedInt(bytes[inicio + 3]);
    }

    private int inteiroLittleEndian(byte[] bytes, int inicio) {
        return Byte.toUnsignedInt(bytes[inicio])
                | Byte.toUnsignedInt(bytes[inicio + 1]) << 8
                | Byte.toUnsignedInt(bytes[inicio + 2]) << 16
                | Byte.toUnsignedInt(bytes[inicio + 3]) << 24;
    }

    private int inteiro24LittleEndian(byte[] bytes, int inicio) {
        return Byte.toUnsignedInt(bytes[inicio])
                | Byte.toUnsignedInt(bytes[inicio + 1]) << 8
                | Byte.toUnsignedInt(bytes[inicio + 2]) << 16;
    }

    private int lerUnsignedShort(InputStream input) throws IOException {
        int primeiro = input.read();
        int segundo = input.read();
        if (primeiro == -1 || segundo == -1) {
            throw new IllegalArgumentException("Arquivo de imagem incompleto.");
        }
        return primeiro << 8 | segundo;
    }

    private boolean ehMarcadorDimensoesJpeg(int marcador) {
        return marcador >= 0xC0
                && marcador <= 0xCF
                && marcador != 0xC4
                && marcador != 0xC8
                && marcador != 0xCC;
    }

    private TipoImagem identificarAssinatura(Path imagem) {
        try (InputStream input = Files.newInputStream(imagem, LinkOption.NOFOLLOW_LINKS)) {
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

    private record Dimensoes(int largura, int altura) {
    }
}
