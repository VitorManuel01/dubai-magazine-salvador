package com.ecommerceproject.dubaimagazinesalvador.services.produto;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;

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
                new byte[]{
                        'R', 'I', 'F', 'F', 4, 0, 0, 0,
                        'W', 'E', 'B', 'P'
                }
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
}
