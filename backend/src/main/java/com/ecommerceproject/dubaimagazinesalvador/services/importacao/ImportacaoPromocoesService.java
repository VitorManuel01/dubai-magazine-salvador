package com.ecommerceproject.dubaimagazinesalvador.services.importacao;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Types;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.ecommerceproject.dubaimagazinesalvador.domain.importacao.ImportacaoPromocoesResponseDTO;
import com.ecommerceproject.dubaimagazinesalvador.domain.importacao.PromocaoProdutoImportacaoDTO;
import com.ecommerceproject.dubaimagazinesalvador.domain.importacao.RelacaoPromocoesOdsDTO;

@Service
public class ImportacaoPromocoesService {

    private static final long TAMANHO_MAXIMO_ARQUIVO = 20L * 1024L * 1024L;
    private static final String MIME_ODS = "application/vnd.oasis.opendocument.spreadsheet";
    private static final int TAMANHO_LOTE = 1_000;
    private static final String ATUALIZAR_PROMOCAO = """
            UPDATE produtos
            SET em_promocao = ?,
                data_inicial_prom = ?,
                data_final_prom = ?,
                porc_margem = ?,
                porc_desconto = ?,
                preco_promocao = ?,
                especial = ?
            WHERE codigo_santri = ?
            """;

    private final LeitorRelacaoProdutosOds leitorOds;
    private final JdbcTemplate jdbcTemplate;

    public ImportacaoPromocoesService(
            LeitorRelacaoProdutosOds leitorOds,
            JdbcTemplate jdbcTemplate
    ) {
        this.leitorOds = leitorOds;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public ImportacaoPromocoesResponseDTO importar(MultipartFile arquivo) {
        validarArquivo(arquivo);
        long inicio = System.nanoTime();
        LocalDateTime importadoEm = LocalDateTime.now();
        RelacaoPromocoesOdsDTO relacao = lerArquivo(arquivo);

        Set<String> existentes = new HashSet<>(
                jdbcTemplate.queryForList("SELECT codigo_santri FROM produtos", String.class)
        );
        List<PromocaoProdutoImportacaoDTO> encontradas = relacao.promocoes().stream()
                .filter(promocao -> existentes.contains(promocao.codigoSantri()))
                .toList();

        if (encontradas.isEmpty()) {
            throw new ImportacaoOdsException(
                    "Nenhum código Santri do relatório foi encontrado no catálogo atual."
            );
        }

        limparPromocoesAnteriores();
        jdbcTemplate.batchUpdate(
                ATUALIZAR_PROMOCAO,
                encontradas,
                TAMANHO_LOTE,
                (statement, promocao) -> {
                    statement.setBoolean(1, promocao.emPromocao());
                    statement.setObject(2, promocao.dataInicial());
                    statement.setObject(3, promocao.dataFinal());
                    if (promocao.porcMargem() == null) {
                        statement.setNull(4, Types.DECIMAL);
                    } else {
                        statement.setBigDecimal(4, promocao.porcMargem());
                    }
                    if (promocao.porcDesconto() == null) {
                        statement.setNull(5, Types.DECIMAL);
                    } else {
                        statement.setBigDecimal(5, promocao.porcDesconto());
                    }
                    statement.setBigDecimal(6, promocao.precoPromocao());
                    statement.setBoolean(7, promocao.especial());
                    statement.setString(8, promocao.codigoSantri());
                }
        );

        int ativas = (int) encontradas.stream().filter(PromocaoProdutoImportacaoDTO::emPromocao).count();
        String nomeArquivo = StringUtils.cleanPath(
                arquivo.getOriginalFilename() == null
                        ? "promocoes-venda.ods"
                        : arquivo.getOriginalFilename()
        );
        return new ImportacaoPromocoesResponseDTO(
                nomeArquivo,
                relacao.promocoes().size(),
                encontradas.size(),
                ativas,
                encontradas.size() - ativas,
                relacao.promocoes().size() - encontradas.size(),
                relacao.linhasIgnoradas(),
                importadoEm,
                Duration.ofNanos(System.nanoTime() - inicio).toMillis()
        );
    }

    private void limparPromocoesAnteriores() {
        jdbcTemplate.update("""
                UPDATE produtos
                SET em_promocao = FALSE,
                    data_inicial_prom = NULL,
                    data_final_prom = NULL,
                    porc_margem = NULL,
                    porc_desconto = NULL,
                    preco_promocao = NULL,
                    especial = FALSE
                """);
    }

    private RelacaoPromocoesOdsDTO lerArquivo(MultipartFile arquivo) {
        try (InputStream input = arquivo.getInputStream()) {
            return leitorOds.lerPromocoes(input);
        } catch (IOException e) {
            throw new ImportacaoOdsException("Não foi possível ler o arquivo enviado.", e);
        }
    }

    private void validarArquivo(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new ImportacaoOdsException("Selecione o arquivo ODS de promoções de venda.");
        }
        String nome = arquivo.getOriginalFilename();
        if (nome == null || nome.length() > 255 || nome.indexOf('\0') >= 0
                || !nome.toLowerCase(Locale.ROOT).endsWith(".ods")) {
            throw new ImportacaoOdsException("O arquivo deve possuir a extensão .ods.");
        }
        if (arquivo.getSize() > TAMANHO_MAXIMO_ARQUIVO) {
            throw new ImportacaoOdsException("O arquivo excede o limite de 20 MB.");
        }
        String contentType = arquivo.getContentType();
        if (contentType != null && !contentType.isBlank()
                && !MIME_ODS.equalsIgnoreCase(contentType)
                && !"application/octet-stream".equalsIgnoreCase(contentType)) {
            throw new ImportacaoOdsException("O tipo de arquivo enviado não é ODS.");
        }
        try (InputStream input = arquivo.getInputStream()) {
            byte[] assinatura = input.readNBytes(4);
            if (assinatura.length != 4 || assinatura[0] != 'P' || assinatura[1] != 'K'
                    || assinatura[2] != 3 || assinatura[3] != 4) {
                throw new ImportacaoOdsException("O conteúdo do arquivo não corresponde a um ODS válido.");
            }
        } catch (IOException e) {
            throw new ImportacaoOdsException("Não foi possível validar o arquivo enviado.", e);
        }
    }
}
