package com.ecommerceproject.dubaimagazinesalvador.services.importacao;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.ecommerceproject.dubaimagazinesalvador.domain.importacao.EstoqueProdutoImportacaoDTO;
import com.ecommerceproject.dubaimagazinesalvador.domain.importacao.ImportacaoEstoqueResponseDTO;

@Service
public class ImportacaoEstoqueService {

    private static final long TAMANHO_MAXIMO_ARQUIVO = 20L * 1024L * 1024L;
    private static final String MIME_ODS =
            "application/vnd.oasis.opendocument.spreadsheet";
    private static final int TAMANHO_LOTE = 1_000;
    private static final String ATUALIZAR_ESTOQUE = """
            UPDATE produtos
            SET estoque = ?
            WHERE codigo_santri = ?
            """;

    private final LeitorRelacaoProdutosOds leitorOds;
    private final JdbcTemplate jdbcTemplate;

    public ImportacaoEstoqueService(
            LeitorRelacaoProdutosOds leitorOds,
            JdbcTemplate jdbcTemplate
    ) {
        this.leitorOds = leitorOds;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public ImportacaoEstoqueResponseDTO importar(MultipartFile arquivo) {
        validarArquivo(arquivo);
        long inicio = System.nanoTime();
        LocalDateTime importadoEm = LocalDateTime.now();
        Set<String> codigosExistentes = new HashSet<>(jdbcTemplate.queryForList(
                "SELECT codigo_santri FROM produtos",
                String.class
        ));
        List<EstoqueProdutoImportacaoDTO> lote = new ArrayList<>(TAMANHO_LOTE);
        int[] produtosAtualizados = {0};
        int[] codigosIgnorados = {0};

        int registrosLidos;
        try (InputStream input = arquivo.getInputStream()) {
            registrosLidos = leitorOds.lerInventario(input, registro -> {
                if (!codigosExistentes.contains(registro.codigoSantri())) {
                    codigosIgnorados[0]++;
                    return;
                }
                lote.add(registro);
                produtosAtualizados[0]++;
                if (lote.size() >= TAMANHO_LOTE) {
                    atualizarLote(lote);
                    lote.clear();
                }
            });
            if (!lote.isEmpty()) {
                atualizarLote(lote);
                lote.clear();
            }
        } catch (IOException e) {
            throw new ImportacaoOdsException(
                    "Não foi possível ler o inventário enviado.",
                    e
            );
        }

        String nomeArquivo = StringUtils.cleanPath(
                arquivo.getOriginalFilename() == null
                        ? "inventario.ods"
                        : arquivo.getOriginalFilename()
        );
        return new ImportacaoEstoqueResponseDTO(
                nomeArquivo,
                registrosLidos,
                produtosAtualizados[0],
                codigosIgnorados[0],
                importadoEm,
                Duration.ofNanos(System.nanoTime() - inicio).toMillis()
        );
    }

    private void atualizarLote(List<EstoqueProdutoImportacaoDTO> lote) {
        jdbcTemplate.batchUpdate(
                ATUALIZAR_ESTOQUE,
                lote,
                TAMANHO_LOTE,
                (statement, registro) -> {
                    statement.setBigDecimal(1, registro.quantidade());
                    statement.setString(2, registro.codigoSantri());
                }
        );
    }

    private void validarArquivo(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new ImportacaoOdsException(
                    "Selecione o arquivo ODS do inventário do Santri."
            );
        }
        String nome = arquivo.getOriginalFilename();
        if (nome == null
                || nome.length() > 255
                || nome.indexOf('\0') >= 0
                || !nome.toLowerCase(Locale.ROOT).endsWith(".ods")) {
            throw new ImportacaoOdsException("O arquivo deve possuir a extensão .ods.");
        }
        if (arquivo.getSize() > TAMANHO_MAXIMO_ARQUIVO) {
            throw new ImportacaoOdsException("O arquivo excede o limite de 20 MB.");
        }
        String contentType = arquivo.getContentType();
        if (contentType != null
                && !contentType.isBlank()
                && !MIME_ODS.equalsIgnoreCase(contentType)
                && !"application/octet-stream".equalsIgnoreCase(contentType)) {
            throw new ImportacaoOdsException("O tipo de arquivo enviado não é ODS.");
        }
        validarAssinaturaZip(arquivo);
    }

    private void validarAssinaturaZip(MultipartFile arquivo) {
        try (InputStream input = arquivo.getInputStream()) {
            byte[] assinatura = input.readNBytes(4);
            if (assinatura.length != 4
                    || assinatura[0] != 'P'
                    || assinatura[1] != 'K'
                    || assinatura[2] != 3
                    || assinatura[3] != 4) {
                throw new ImportacaoOdsException(
                        "O conteúdo do arquivo não corresponde a um ODS válido."
                );
            }
        } catch (IOException e) {
            throw new ImportacaoOdsException("Não foi possível validar o arquivo enviado.", e);
        }
    }
}
