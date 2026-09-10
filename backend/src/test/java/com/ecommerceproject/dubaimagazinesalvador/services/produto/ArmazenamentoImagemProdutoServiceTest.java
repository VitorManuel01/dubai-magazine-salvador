package com.ecommerceproject.dubaimagazinesalvador.services.produto;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

class ArmazenamentoImagemProdutoServiceTest {

    @TempDir
    private Path diretorio;

    @Test
    void deveSalvarImagemSemCodigoInternoNoNome() throws Exception {
        ArmazenamentoImagemProdutoService service =
                new ArmazenamentoImagemProdutoService(diretorio.toString());
        MockMultipartFile imagem = new MockMultipartFile(
                "imagem",
                "produto.webp",
                "image/webp",
                webpUmPorUm()
        );

        String url = service.salvar(imagem);

        assertFalse(url.contains("855440"));
    }

    @Test
    void deveLocalizarImagemAntigaPeloIdentificadorPublico() throws Exception {
        String identificador = "7abe08ab-cebd-4dd8-a6a9-6252faa1d9f8.webp";
        Files.write(diretorio.resolve("855440-" + identificador), new byte[]{4, 5, 6});
        ArmazenamentoImagemProdutoService service =
                new ArmazenamentoImagemProdutoService(diretorio.toString());

        byte[] conteudo = service.carregarParaCatalogo(identificador)
                .getInputStream()
                .readAllBytes();

        assertArrayEquals(new byte[]{4, 5, 6}, conteudo);
    }

    @Test
    void deveRejeitarImagemDisfarcadaPorExtensaoEMimeType() {
        ArmazenamentoImagemProdutoService service =
                new ArmazenamentoImagemProdutoService(diretorio.toString());
        MockMultipartFile imagem = new MockMultipartFile(
                "imagem",
                "produto.png",
                "image/png",
                new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff, 0, 0, 0, 0, 0, 0, 0, 0, 0}
        );

        assertThrows(ResponseStatusException.class, () -> service.salvar(imagem));
    }

    @Test
    void deveRejeitarArquivoSemAssinaturaDeImagem() {
        ArmazenamentoImagemProdutoService service =
                new ArmazenamentoImagemProdutoService(diretorio.toString());
        MockMultipartFile imagem = new MockMultipartFile(
                "imagem",
                "produto.webp",
                "image/webp",
                "não é uma imagem".getBytes()
        );

        assertThrows(ResponseStatusException.class, () -> service.salvar(imagem));
    }

    @Test
    void deveRemoverSomenteImagemComNomeGerenciado() throws Exception {
        String identificador = "7abe08ab-cebd-4dd8-a6a9-6252faa1d9f8.webp";
        Path imagem = diretorio.resolve(identificador);
        Files.write(imagem, new byte[] { 1, 2, 3 });
        ArmazenamentoImagemProdutoService service =
                new ArmazenamentoImagemProdutoService(diretorio.toString());

        service.removerSeGerenciada("/catalogo/imagens/" + identificador);

        assertTrue(Files.notExists(imagem));
    }

    @Test
    void devePreservarImagemLegadaAindaReferenciadaNaLimpeza() throws Exception {
        String identificador = "7abe08ab-cebd-4dd8-a6a9-6252faa1d9f8.webp";
        Path imagem = Files.write(diretorio.resolve("855440-" + identificador), new byte[]{1});
        Files.setLastModifiedTime(imagem, FileTime.from(Instant.now().minus(Duration.ofDays(3))));
        var service = new ArmazenamentoImagemProdutoService(diretorio.toString());

        String referencia = service.nomeGerenciado("/uploads/produtos/855440-" + identificador);

        assertEquals(identificador, referencia);
        assertEquals(0, service.removerOrfas(Set.of(referencia), Duration.ofDays(1)));
        assertTrue(Files.exists(imagem));
        assertEquals(1, service.removerOrfas(Set.of(), Duration.ofDays(1)));
    }

    @Test
    void deveRejeitarTamanhoRealExcessivoMesmoQuandoDeclaradoPequeno() throws Exception {
        var service = new ArmazenamentoImagemProdutoService(diretorio.toString());
        byte[] conteudo = new byte[5 * 1024 * 1024 + 1];
        System.arraycopy(webpUmPorUm(), 0, conteudo, 0, webpUmPorUm().length);
        MockMultipartFile imagem = new MockMultipartFile("imagem", "foto.webp", "image/webp", conteudo) {
            @Override
            public long getSize() { return 30; }
        };

        ResponseStatusException erro = assertThrows(ResponseStatusException.class, () -> service.salvar(imagem));

        assertEquals(413, erro.getStatusCode().value());
        try (var arquivos = Files.list(diretorio)) {
            assertEquals(0, arquivos.count());
        }
    }

    @Test
    void deveRemoverArquivoParcialQuandoLeituraFalha() throws Exception {
        var service = new ArmazenamentoImagemProdutoService(diretorio.toString());
        MockMultipartFile imagem = new MockMultipartFile("imagem", "foto.webp", "image/webp", webpUmPorUm()) {
            @Override
            public InputStream getInputStream() {
                return new InputStream() {
                    private int lidos;
                    @Override
                    public int read() throws IOException {
                        if (++lidos > 32) { throw new IOException("Falha de leitura simulada"); }
                        return 1;
                    }
                };
            }
        };

        assertThrows(ResponseStatusException.class, () -> service.salvar(imagem));
        try (var arquivos = Files.list(diretorio)) {
            assertEquals(0, arquivos.count());
        }
    }

    @Test
    void deveValidarEPersistirUmaUnicaLeituraDoUpload() throws Exception {
        var service = new ArmazenamentoImagemProdutoService(diretorio.toString());
        byte[] valido = webpUmPorUm();
        MockMultipartFile imagem = new MockMultipartFile("imagem", "foto.webp", "image/webp", valido) {
            private int aberturas;
            @Override
            public InputStream getInputStream() {
                if (++aberturas > 1) { throw new AssertionError("Upload foi reaberto após validação"); }
                return new ByteArrayInputStream(valido);
            }
        };

        String url = service.salvar(imagem);

        assertArrayEquals(valido, Files.readAllBytes(diretorio.resolve(service.nomeGerenciado(url))));
    }

    @Test
    void deveRecusarDiretorioQueAtravesseLinkSimbolico() throws Exception {
        Path destinoReal = Files.createDirectory(diretorio.resolve("real"));
        Path link = diretorio.resolve("link");
        try {
            Files.createSymbolicLink(link, destinoReal);
        } catch (IOException | UnsupportedOperationException e) {
            Assumptions.abort("O ambiente não permite criar links simbólicos.");
        }

        assertThrows(IllegalStateException.class,
                () -> new ArmazenamentoImagemProdutoService(link.resolve("produtos").toString()));
        assertFalse(Files.exists(destinoReal.resolve("produtos")));
    }

    private byte[] webpUmPorUm() {
        return new byte[]{
                'R', 'I', 'F', 'F', 22, 0, 0, 0,
                'W', 'E', 'B', 'P',
                'V', 'P', '8', 'X', 10, 0, 0, 0,
                0, 0, 0, 0,
                0, 0, 0,
                0, 0, 0
        };
    }
}
